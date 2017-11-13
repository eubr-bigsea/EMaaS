package recordLinkageDF;

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
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.clearspring.analytics.util.Lists;

import BULMADependences.Problem;
import PointDependencies.ShapePoint;
import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.BulmaOutputGrouping;
import recordLinkage.dependencies.Comparator;
import recordLinkage.dependencies.ShapeLine;
import recordLinkage.dependencies.TicketInformation;
import scala.Tuple2;

public class GenerateDataFrameBUSTE {
	
	private static final String SEPARATOR = ",";
	private static final String HEADER = "route,tripNum,shapeId,shapeSequence,shapeLat,shapeLon,distanceTraveledShape,busCode,"
			+ "gpsPointId,gpsLat,gpsLon,distanceToShapePoint,timestamp,stopPointId,problem,birthdate,"
			+ "cardTimestamp,lineName,cardNum,cardNum";
	
	@SuppressWarnings("serial")
	public static Dataset<Tuple2<String, Object>> generateDataFrames(JavaRDD<Row> shapeFileDS,
			JavaRDD<Row> busStopsFile, JavaRDD<Row> bulmaOutputRow, Integer minPartitions, SparkSession spark) throws Exception {
		
		Encoder<Object> encoder = Encoders.javaSerialization(Object.class);
		
		JavaPairRDD<String, Iterable<BulmaOutput>> rddBulmaOutpupGrouped = bulmaOutputRow.mapToPair(new PairFunction<Row, String, BulmaOutput>() {

			@Override
			public Tuple2<String, BulmaOutput> call(Row t) throws Exception {
				StringTokenizer st = new StringTokenizer(t.getString(0), SEPARATOR);
				System.out.println(t.toString());
				System.out.println(t.getString(0));
				BulmaOutput bulmaOutput = new BulmaOutput(st.nextToken(), st.nextToken(), st.nextToken(),
						st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
						st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
						st.nextToken());

				String key = bulmaOutput.getBusCode() + ":" + bulmaOutput.getTripNum() + ":"
						+ bulmaOutput.getShapeId();

				return new Tuple2<String, BulmaOutput>(key, bulmaOutput);
			}
		}).groupByKey(minPartitions);
		
		JavaPairRDD<String, Iterable<ShapePoint>> rddShapePointsGrouped = shapeFileDS.mapToPair(new PairFunction<Row, String, ShapePoint>() {

			@Override
			public Tuple2<String, ShapePoint> call(Row t) throws Exception {
				ShapePoint shapePoint = ShapePoint.createShapePointRoute(t.getString(0));
				return new Tuple2<String, ShapePoint>(shapePoint.getId(), shapePoint);
			}
			
		}).groupByKey(minPartitions);
		
		JavaPairRDD<String, Object> rddBusStops = busStopsFile.mapToPair(new PairFunction<Row, String, Object>() {

			@Override
			public Tuple2<String, Object> call(Row t) throws Exception {
				String[] splittedEntry = t.getString(0).split(SEPARATOR);
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
		
		JavaPairRDD<String, Object> rddUnion = rddBulmaOutputGrouping.union(rddShapeLinePair)
				.union(rddBusStops);
		Dataset<Tuple2<String, Object>> dataset = spark.createDataset(JavaPairRDD.toRDD(rddUnion), Encoders.tuple(Encoders.STRING(), encoder));
		
		return dataset;
	}
	
	@SuppressWarnings({ "resource", "serial" })
	public static Dataset<String> run(Dataset<Tuple2<String, Object>> dataset, Integer minPartitions, SparkSession spark,
			Map<String, List<TicketInformation>> mapTickets) throws Exception {
		
		JavaSparkContext cont = new JavaSparkContext(spark.sparkContext());
		final Broadcast<Map<String, List<TicketInformation>>> mapTicketsBroadcast = cont.broadcast(mapTickets);
		
		JavaRDD<Tuple2<String, Object>> rddUnionLines = dataset.toJavaRDD();

		JavaPairRDD<String, Iterable<Object>> rddGroupedUnionLines = rddUnionLines
				.mapToPair(new PairFunction<Tuple2<String, Object>, String, Object>() {

					public Tuple2<String, Object> call(Tuple2<String, Object> t) throws Exception {

						return new Tuple2<String, Object>(t._1, t._2);
					}
				}).groupByKey(minPartitions);
		
		JavaRDD<String> rddInterpolation = rddGroupedUnionLines
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
		
		Function2<Integer, Iterator<String>, Iterator<String>> insertHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {

			@SuppressWarnings("unchecked")
			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				List<String> output = new LinkedList<String>();
				output.add(HEADER);
				output.addAll(IteratorUtils.toList(iterator));
				
				return output.iterator();
			}
		};
		
		JavaRDD<String> rddOutputWithHeader = rddOutput.mapPartitionsWithIndex(insertHeader, false);

		return spark.createDataset(rddOutputWithHeader.rdd(), Encoders.STRING());
	}
}