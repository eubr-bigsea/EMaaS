package recordLinkage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.commons.collections.IteratorUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;

import com.clearspring.analytics.util.Lists;

import BULMADependences.Problem;
import PointDependencies.ShapePoint;
import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.BulmaOutputGrouping;
import recordLinkage.dependencies.Comparator;
import recordLinkage.dependencies.ShapeLine;
import recordLinkage.dependencies.TicketInformation;
import scala.Tuple2;

/**
 * (Spark implementation version)
 * 
 * This class does the post-processing of Bulma, get its output and interpolates
 * the shape file. Besides, this class add the stop points file to the output.
 * In this version (V3), this class includes the bus tickets file to the output and returns only the lines with busStop.
 * 
 * @author Andreza
 *
 */
public class BUSTEstimationV3 {

	private static final String SEPARATOR = ",";
	private static final String SLASH = "/";

	public static void main(String[] args) throws IOException, URISyntaxException {

		if (args.length < 6) {
			System.err.println(
					"Usage: <Output Bulma directory> <shape file>  <Bus stops file> <Bus tickets directory> <outputPath> <number of partitions>");
			System.exit(1);
		}
		Long initialTime = System.currentTimeMillis();

		String pathBulmaOutput = args[0];
		String pathFileShapes = args[1];
		String busStopsFile = args[2];
		String busTicketPath = args[3];
		String outputPath = args[4];
		final Integer minPartitions = Integer.valueOf(args[5]);

//		SparkConf sparkConf = new SparkConf().setAppName("BUSTEstimationV3").setMaster("local");
		SparkConf sparkConf = new SparkConf().setAppName("BUSTEstimationV3");
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFilesHDFS(context, pathBulmaOutput, pathFileShapes, busStopsFile, busTicketPath, outputPath,
				minPartitions);

		context.stop();
		context.close();
		System.out.println("Execution time: " + (System.currentTimeMillis() - initialTime));
	}

	private static void generateOutputFilesHDFS(JavaSparkContext context, String pathBulmaOutput, String pathFileShapes,
			String busStopsFile, String busTicketPath, String output, int minPartitions)
			throws IOException, URISyntaxException {

		Function2<Integer, Iterator<String>, Iterator<String>> removeEmptyLines = new Function2<Integer, Iterator<String>, Iterator<String>>() {

			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				List<String> output = new LinkedList<String>();
				String line;
				while (iterator.hasNext()) {
					line = iterator.next();
					if (!line.isEmpty()) {
						output.add(line);
					}

				}
				return output.iterator();

			}
		};

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(new URI(pathBulmaOutput), conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(pathBulmaOutput));

		for (FileStatus file : fileStatus) {

			FileStatus[] fileStatus2 = fs.listStatus(new Path(pathBulmaOutput + SLASH + file.getPath().getName()));

			String pathDir = pathBulmaOutput + SLASH + file.getPath().getName();

			JavaRDD<String> bulmaOutputString = context.textFile(pathDir + SLASH + "part-00000");

			for (FileStatus filePart : fileStatus2) {
				if (!filePart.getPath().getName().equals("_SUCCESS")
						&& !filePart.getPath().getName().equals("part-00000")) {
					bulmaOutputString
							.union(context.textFile(pathDir + SLASH + filePart.getPath().getName(), minPartitions));
				}
			}
			bulmaOutputString = bulmaOutputString.mapPartitionsWithIndex(removeEmptyLines, false);

			String ticketPathFile = busTicketPath + SLASH + "doc1-" + file.getPath().getName().substring(0,  file.getPath().getName().lastIndexOf("_veiculos")) + ".csv";
			JavaRDD<String> result = execute(context, bulmaOutputString, pathFileShapes, ticketPathFile, busStopsFile,
					minPartitions);
			
			Function2<Integer, Iterator<String>, Iterator<String>> insertHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

				public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
					List<String> output = new LinkedList<String>();
					output.add("route,tripNum,shapeId,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,busCode,gpsPointId,gpsLat,gpsLon,distanceToShapePoint,timestamp,stopPointId,problem,birthdate,cardTimestamp,lineName,cardNum,cardNum");
					output.addAll(IteratorUtils.toList(iterator));
					
					return output.iterator();

				}
			};
			
			result.mapPartitionsWithIndex(insertHeader, false).saveAsTextFile(output + SLASH + file.getPath().getName());
		}

	}

	@SuppressWarnings("serial")
	private static JavaRDD<String> execute(JavaSparkContext context, JavaRDD<String> bulmaOutputString,
			String pathFileShapes, String busTicketFile, String busStopsFile, int minPartitions) {

		Map<String, List<TicketInformation>> mapTickets = new HashMap<String, List<TicketInformation>>();

		Function2<Integer, Iterator<String>, Iterator<String>> removeHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				if (index == 0 && iterator.hasNext()) {
					iterator.next();
					return iterator;
				} else {
					return iterator;
				}
			}
		};

		JavaRDD<String> busTicketsString = context.textFile(busTicketFile, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<TicketInformation>> rddTicketsMapped = busTicketsString
				.mapToPair(new PairFunction<String, String, TicketInformation>() {

					public Tuple2<String, TicketInformation> call(String entry) throws Exception {

						entry += " ";
						String[] splittedEntry = entry.split("(?<=" + SEPARATOR + ")");

						TicketInformation ticket = new TicketInformation(splittedEntry[0].replace(SEPARATOR, ""),
								splittedEntry[1].replace(SEPARATOR, ""), splittedEntry[2].replace(SEPARATOR, ""),
								splittedEntry[3].replace(SEPARATOR, ""), splittedEntry[4].replace(SEPARATOR, ""),
								splittedEntry[5].replace(SEPARATOR, ""), splittedEntry[6].replace(SEPARATOR, ""),
								splittedEntry[7].replace(SEPARATOR, ""));

						return new Tuple2<String, TicketInformation>(ticket.getBusCode(), ticket);
					}
				}).groupByKey();

		for (Tuple2<String, Iterable<TicketInformation>> entry : rddTicketsMapped.collect()) {
			mapTickets.put(entry._1, Lists.newArrayList(entry._2));
		}
		final Broadcast<Map<String, List<TicketInformation>>> mapTicketsBroadcast = context.broadcast(mapTickets);

		JavaRDD<String> shapeString = context.textFile(pathFileShapes, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaRDD<String> busStopsString = context.textFile(busStopsFile, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<BulmaOutput>> rddBulmaOutpupGrouped = bulmaOutputString
				.mapToPair(new PairFunction<String, String, BulmaOutput>() {

					public Tuple2<String, BulmaOutput> call(String t) throws Exception {
						StringTokenizer st = new StringTokenizer(t, SEPARATOR);
						BulmaOutput bulmaOutput = new BulmaOutput(st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken());

						String key = bulmaOutput.getBusCode() + ":" + bulmaOutput.getTripNum() + ":"
								+ bulmaOutput.getShapeId();

						return new Tuple2<String, BulmaOutput>(key, bulmaOutput);
					}
				}).groupByKey(minPartitions);

		JavaPairRDD<String, Iterable<ShapePoint>> rddShapePointsGrouped = shapeString
				.mapToPair(new PairFunction<String, String, ShapePoint>() {

					public Tuple2<String, ShapePoint> call(String s) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(s);
						return new Tuple2<String, ShapePoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey(minPartitions);

		JavaPairRDD<String, Object> rddBusStops = busStopsString.mapToPair(new PairFunction<String, String, Object>() {

			public Tuple2<String, Object> call(String entry) throws Exception {
				String[] splittedEntry = entry.split(SEPARATOR);
				// shapeID , shapeSequence + '.' + stopId
				return new Tuple2<String, Object>(splittedEntry[7], splittedEntry[8] + "." + splittedEntry[2]);
			}
		});

		JavaPairRDD<String, Object> rddBulmaOutputGrouping = rddBulmaOutpupGrouped
				.mapToPair(new PairFunction<Tuple2<String, Iterable<BulmaOutput>>, String, Object>() {

					public Tuple2<String, Object> call(Tuple2<String, Iterable<BulmaOutput>> t) throws Exception {
						Map<String, BulmaOutput> mapOutputGrouping = new HashMap<String, BulmaOutput>();
						String key = t._1.split("\\:")[2]; // [2] = shapeId

						for (BulmaOutput bulmaOutput : t._2) {
							mapOutputGrouping.put(bulmaOutput.getShapeSequence(), bulmaOutput);
						}

						return new Tuple2<String, Object>(key, new BulmaOutputGrouping(mapOutputGrouping));
					}
				});

		JavaPairRDD<String, Object> rddShapeLinePair = rddShapePointsGrouped
				.mapToPair(new PairFunction<Tuple2<String, Iterable<ShapePoint>>, String, Object>() {

					public Tuple2<String, Object> call(Tuple2<String, Iterable<ShapePoint>> pair) throws Exception {

						LinkedList<ShapePoint> listShapePoints = new LinkedList<ShapePoint>();
						Iterator<ShapePoint> it = pair._2.iterator();
						while (it.hasNext()) {
							listShapePoints.add(it.next());
						}

						String route = listShapePoints.get(listShapePoints.size() - 1).getRoute();
						ShapeLine shapeLine = new ShapeLine(pair._1, listShapePoints, route);

						return new Tuple2<String, Object>(pair._1, shapeLine);
					}
				});

		JavaPairRDD<String, Iterable<Object>> rddUnion = rddBulmaOutputGrouping.union(rddShapeLinePair)
				.union(rddBusStops).groupByKey(minPartitions);

		JavaRDD<String> rddInterpolation = rddUnion
				.flatMap(new FlatMapFunction<Tuple2<String, Iterable<Object>>, String>() {

					private ShapeLine shapeLine;
					private List<BulmaOutputGrouping> listBulmaOutputGrouping;
					private Map<String, String> mapStopPoints; // Map<ShapeSequence,StopPointId>
					private Map<String, String> mapAux;

					public Iterator<String> call(Tuple2<String, Iterable<Object>> t) throws Exception {
						List<String> listOutput = new LinkedList<String>();

						shapeLine = null;
						listBulmaOutputGrouping = new ArrayList<BulmaOutputGrouping>();
						mapStopPoints = new HashMap<String, String>();
						mapAux = new HashMap<String, String>();

						List<Object> listInput = Lists.newArrayList(t._2);
						for (Object obj : listInput) {
							if (obj instanceof BulmaOutputGrouping) {
								listBulmaOutputGrouping.add((BulmaOutputGrouping) obj);
							} else if (obj instanceof ShapeLine) {
								if (shapeLine != null) {
									System.out.println("Error");
								}
								shapeLine = (ShapeLine) obj;
							} else {
								String shapeSequenceStopid = (String) obj;
								String[] splittedObj = shapeSequenceStopid.split("\\.");

								mapStopPoints.put(splittedObj[0], splittedObj[1]);
								mapAux.put(splittedObj[0], splittedObj[1]);
							}
						}

						if (shapeLine == null) {
							return listOutput.iterator();
						}

						for (BulmaOutputGrouping bulmaOutputGrouping : listBulmaOutputGrouping) {
							Tuple2<Float, String> previousGPSPoint = null;
							Tuple2<Float, String> nextGPSPoint = null;
							List<Integer> pointsBetweenGPS = new LinkedList<Integer>();

							String tripNum = "-";

							for (int i = 0; i < shapeLine.getListGeoPoint().size(); i++) {
								ShapePoint currentShapePoint = shapeLine.getListGeoPoint().get(i);
								String currentShapeSequence = currentShapePoint.getPointSequence();
								String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();
								String currentShapeId = shapeLine.getShapeId();
								String currentLatShape = currentShapePoint.getLatitude();
								String currentLonShape = currentShapePoint.getLongitude();
								String currentRoute = shapeLine.getRoute();

								String currentTimestamp;

								if (previousGPSPoint == null) {
									if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {

										BulmaOutput currentOutput = bulmaOutputGrouping.getMapOutputGrouping()
												.get(currentShapeSequence);

										currentTimestamp = currentOutput.getTimestamp();
										previousGPSPoint = new Tuple2<Float, String>(
												currentShapePoint.getDistanceTraveled(), currentTimestamp);

										String busCode = currentOutput.getBusCode();
										String gpsPointId = currentOutput.getGpsPointId();
										String problemCode = currentOutput.getTripProblem();
										tripNum = currentOutput.getTripNum();
										String latGPS = currentOutput.getLatGPS();
										String lonGPS = currentOutput.getLonGPS();
										String distanceToShape = currentOutput.getDinstance();

										addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
												currentLatShape, currentLonShape, currentDistanceTraveled, busCode,
												gpsPointId, latGPS, lonGPS, distanceToShape, currentTimestamp,
												problemCode, listOutput);

									} else {
										addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
												currentLatShape, currentLonShape, currentDistanceTraveled, "-", "-",
												"-", "-", "-", "-", "-", listOutput);
									}
								} else {

									if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {
										BulmaOutput currentOutput = bulmaOutputGrouping.getMapOutputGrouping()
												.get(currentShapeSequence);

										String busCode = currentOutput.getBusCode();
										String gpsPointId = currentOutput.getGpsPointId();
										String problemCode = currentOutput.getTripProblem();
										tripNum = currentOutput.getTripNum();
										String latGPS = currentOutput.getLatGPS();
										String lonGPS = currentOutput.getLonGPS();
										String distanceToShape = currentOutput.getDinstance();
										currentTimestamp = currentOutput.getTimestamp();

										nextGPSPoint = new Tuple2<Float, String>(
												currentShapePoint.getDistanceTraveled(), currentTimestamp);

										generateOutputFromPointsInBetween(currentShapeId, tripNum, previousGPSPoint,
												pointsBetweenGPS, nextGPSPoint, shapeLine.getListGeoPoint(), busCode,
												listOutput);

										addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
												currentLatShape, currentLonShape, currentDistanceTraveled, busCode,
												gpsPointId, latGPS, lonGPS, distanceToShape, currentTimestamp,
												problemCode, listOutput);

										previousGPSPoint = nextGPSPoint;
										nextGPSPoint = null;
										pointsBetweenGPS = new LinkedList<Integer>();

									} else {
										pointsBetweenGPS.add(i);
									}
								}
							}

							if (!pointsBetweenGPS.isEmpty()) {
								for (Integer indexPointsInBetween : pointsBetweenGPS) {

									ShapePoint currentShapePoint = shapeLine.getListGeoPoint()
											.get(indexPointsInBetween);
									String currentShapeSequence = currentShapePoint.getPointSequence();
									String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();
									String currentShapeId = shapeLine.getShapeId();

									String currentLatShape = currentShapePoint.getLatitude();
									String currentLonShape = currentShapePoint.getLongitude();
									String currentRoute = shapeLine.getRoute();

									addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
											currentLatShape, currentLonShape, currentDistanceTraveled, "-", "-", "-",
											"-", "-", "-", "-", listOutput);

								}
							}
						}

						// Condition to verify if there is stop point without
						// correspondence with the shape
						if (listBulmaOutputGrouping.size() > 0 && mapAux != null && mapAux.size() > 0) {
							for (Entry<String, String> entry : mapAux.entrySet()) {
								System.out.println(
										"shapeSequence: " + entry.getKey() + ';' + "stopId: " + entry.getValue());
							}
						}

						return listOutput.iterator();
					}

					private void addOutput(String route, String tripNum, String shapeId, String shapeSequence,
							String shapeLat, String shapeLon, String distanceTraveledShape, String busCode,
							String gpsPointId, String gpsLat, String gpsLon, String distanceToShapePoint,
							String timestamp, String problemCode, List<String> listOutput) {

						String stopPointId = mapStopPoints.get(shapeSequence);
						mapAux.remove(shapeSequence);
						if (stopPointId != null) {
							String problem;

							try {
								problem = Problem.getById(Integer.valueOf(problemCode));
							} catch (Exception e) {
								problem = "BETWEEN";
							}

							String outputString = route + SEPARATOR + tripNum + SEPARATOR + shapeId + SEPARATOR
									+ shapeSequence + SEPARATOR + shapeLat + SEPARATOR + shapeLon + SEPARATOR
									+ distanceTraveledShape + SEPARATOR + busCode + SEPARATOR + gpsPointId + SEPARATOR
									+ gpsLat + SEPARATOR + gpsLon + SEPARATOR + distanceToShapePoint + SEPARATOR + timestamp
									+ SEPARATOR + stopPointId + SEPARATOR + problem;

							listOutput.add(outputString);
						} 
						
					}

					private void generateOutputFromPointsInBetween(String shapeId, String tripNum,
							Tuple2<Float, String> previousGPSPoint, List<Integer> pointsBetweenGPS,
							Tuple2<Float, String> nextGPSPoint, List<ShapePoint> listGeoPointsShape, String busCode,
							List<String> listOutput) throws ParseException {

						Float previousDistanceTraveled = previousGPSPoint._1;
						long previousTime = getTimeLong(previousGPSPoint._2);
						Float nextDistanceTraveled = nextGPSPoint._1;
						long nextTime = getTimeLong(nextGPSPoint._2);
						Float distanceTraveled = nextDistanceTraveled - previousDistanceTraveled;
						long time = nextTime - previousTime;

						Float currentDistanceTraveled;
						long generatedTimeDifference;
						long generatedTime;
						String generatedTimeString;
						String sequence;
						String distance;
						String latShape;
						String lonShape;
						String route;
						for (Integer indexPointsInBetween : pointsBetweenGPS) {

							currentDistanceTraveled = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled()
									- previousDistanceTraveled;
							generatedTimeDifference = (long) ((currentDistanceTraveled * time) / distanceTraveled);
							generatedTime = previousTime + generatedTimeDifference;
							generatedTimeString = getTimeString(generatedTime);
							sequence = listGeoPointsShape.get(indexPointsInBetween).getPointSequence();
							latShape = listGeoPointsShape.get(indexPointsInBetween).getLatitude();
							lonShape = listGeoPointsShape.get(indexPointsInBetween).getLongitude();
							route = listGeoPointsShape.get(indexPointsInBetween).getRoute();
							distance = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled().toString();

							addOutput(route, tripNum, shapeId, sequence, latShape, lonShape, distance, busCode, "-",
									"-", "-", "-", generatedTimeString, "-", listOutput);
						}

					}

					private String getTimeString(long generatedTime) {
						Date date = new Date(generatedTime);
						DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
						return formatter.format(date);
					}

					private long getTimeLong(String timestamp) throws ParseException {
						SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
						return parser.parse(timestamp).getTime();
					}
				});

		JavaPairRDD<String, Iterable<Tuple2<String, String>>> rddMapInterpolation = rddInterpolation
				.mapToPair(new PairFunction<String, String, String>() {

					public Tuple2<String, String> call(String t) throws Exception {
						String[] splittedEntry = t.split(SEPARATOR);
						return new Tuple2<String, String>(splittedEntry[7] + SEPARATOR + splittedEntry[12], t);
					}
				}).sortByKey(new Comparator()).groupBy(new Function<Tuple2<String, String>, String>() {

					public String call(Tuple2<String, String> v1) throws Exception {
						String key = v1._1.split(SEPARATOR)[0];
						return key;
					}
				}, minPartitions);
		
		JavaRDD<String> rddOutput = rddMapInterpolation
				.flatMap(new FlatMapFunction<Tuple2<String, Iterable<Tuple2<String, String>>>, String>() {

					public Iterator<String> call(Tuple2<String, Iterable<Tuple2<String, String>>> t) throws Exception {

						List<String> listOutput = new LinkedList<String>();
						List<Tuple2<String, String>> list = Lists.newArrayList(t._2);
						String currentBusCode = t._1;
						String nextTimeString = null;
						for (int i = list.size() - 1; i >= 0; i--) {
							String currentString = list.get(i)._2;
							String currentBusStop = currentString.split(SEPARATOR)[13];

							if (!currentBusStop.equals("-")) {
								String currentTimeString = currentString.split(SEPARATOR)[12];
								if (!currentTimeString.equals("-")) {
									if (nextTimeString == null) {
										nextTimeString = currentTimeString;
										listOutput.add(0, currentString
											+ SEPARATOR + "-"
											+ SEPARATOR + "-"
											+ SEPARATOR + "-"
											+ SEPARATOR + "-"
											+ SEPARATOR + "-");

									} else {
										List<TicketInformation> selectedTickets = getNumberTicketsOfBusStop(currentBusCode, currentTimeString,
												nextTimeString);
										
										if (selectedTickets.size() == 0) {
											listOutput.add(0, currentString
													+ SEPARATOR + "-"
													+ SEPARATOR + "-"
													+ SEPARATOR + "-"
													+ SEPARATOR + "-"
													+ SEPARATOR + "-");
										}
										
										for (TicketInformation selectedTicket : selectedTickets) {
											listOutput.add(0, currentString 
												+ SEPARATOR + selectedTicket.getBirthDate()
												+ SEPARATOR + selectedTicket.getTimeOfUse()
												+ SEPARATOR + selectedTicket.getNameLine()
												+ SEPARATOR + selectedTicket.getTicketNumber()
												+ SEPARATOR + selectedTicket.getGender());

										}
										nextTimeString = currentTimeString;
										
										
									}
								} else {
									listOutput.add(0, currentString
											+ SEPARATOR + "-"
											+ SEPARATOR + "-"
											+ SEPARATOR + "-"
											+ SEPARATOR + "-"
											+ SEPARATOR + "-");
								}

							} 
							
						}

						return listOutput.iterator();
					}

					private List<TicketInformation> getNumberTicketsOfBusStop(String currentBusCode, String currentTimeString,
							String nextTimeString) throws ParseException {

						SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
						Date currentTime = sdf.parse(currentTimeString);
						Date nextTime = sdf.parse(nextTimeString);
//						int count = 0;
						List<TicketInformation> ticketsInformationList = mapTicketsBroadcast.getValue().get(currentBusCode);
						List<TicketInformation> listOutput = new LinkedList<>();
						
						if (ticketsInformationList != null) {
							for (TicketInformation TicketInformation : ticketsInformationList) {
								String timeString = TicketInformation.getTimeOfUse();
								Date date = sdf.parse(timeString);
								if (date.after(currentTime) && (date.before(nextTime) || date.equals(nextTime))) {
//									count++;
									listOutput.add(TicketInformation);
								}
							}
						}

						return listOutput;

					}

				});

		return rddOutput;

	}

}
