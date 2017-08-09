package recordLinkage;

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

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import com.clearspring.analytics.util.Lists;

import PointDependencies.Problem;
import PointDependencies.ShapePoint;
import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.BulmaOutputGrouping;
import recordLinkage.dependencies.ShapeLine;
import scala.Tuple2;

public class BUSTEstimation {

	private static final String FILE_SEPARATOR = ",";

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 4) {
			System.err.println("Usage: <Output Bulma file> <shape file>  <Bus stops file> <number of partitions>");
			System.exit(1);
		}
		String pathBulmaOutputFile = args[0];
		String pathFileShapes = args[1];
		String busStopsFile = args[2];
		final Integer minPartitions = Integer.valueOf(args[3]);

		SparkConf sparkConf = new SparkConf().setAppName("BUSTEstimation").setMaster("local");
		// SparkConf sparkConf = new SparkConf().setAppName("BUSTEstimation");
		JavaSparkContext context = new JavaSparkContext(sparkConf);

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

		JavaRDD<String> bulmaOutputString = context.textFile(pathBulmaOutputFile, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaRDD<String> shapeString = context.textFile(pathFileShapes, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaRDD<String> busStopsString = context.textFile(busStopsFile, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<BulmaOutput>> rddBulmaOuptupGrouped = bulmaOutputString
				.mapToPair(new PairFunction<String, String, BulmaOutput>() {

					public Tuple2<String, BulmaOutput> call(String t) throws Exception {
						StringTokenizer st = new StringTokenizer(t, FILE_SEPARATOR);
						BulmaOutput bulmaOutput = new BulmaOutput(st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(), st.nextToken(),
								st.nextToken());

						String key = bulmaOutput.getShapeId() + ":" + bulmaOutput.getBusCode() + ":"
								+ bulmaOutput.getTripNum();

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
				String[] splittedEntry = entry.split(FILE_SEPARATOR);
				                                        //shapeID , shapeSequence + '.' + stopId
				return new Tuple2<String, Object>(splittedEntry[7], splittedEntry[8] +"."+ splittedEntry[2]); 
			}
		});

		JavaPairRDD<String, Object> rddBulmaOutputGrouping = rddBulmaOuptupGrouped
				.mapToPair(new PairFunction<Tuple2<String, Iterable<BulmaOutput>>, String, Object>() {

					public Tuple2<String, Object> call(Tuple2<String, Iterable<BulmaOutput>> t) throws Exception {
						String key = t._1.split("\\:")[0];
						Map<String, BulmaOutput> mapOutputGrouping = new HashMap<String, BulmaOutput>();

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
					private Map<String, String> mapStopPoints;

					public Iterator<String> call(Tuple2<String, Iterable<Object>> t) throws Exception {
						List<String> listOutput = new LinkedList<String>();

						shapeLine = null;
						listBulmaOutputGrouping = new ArrayList<BulmaOutputGrouping>();
						mapStopPoints = new HashMap<String, String>();
						
						List<Object> listInput = Lists.newArrayList(t._2);
						for (Object obj : listInput) {
							if (obj instanceof BulmaOutputGrouping) {
								listBulmaOutputGrouping.add((BulmaOutputGrouping) obj);
							} else if (obj instanceof ShapeLine) {
								shapeLine = (ShapeLine) obj;
							} else {
								String shapeSequenceStopid = (String) obj;
								String[] splittedObj = shapeSequenceStopid.split("\\.");
								
								mapStopPoints.put(splittedObj[0], splittedObj[1]);
							}
						}
						
						if (shapeLine == null) {
							return listOutput.iterator();
						}

						for (BulmaOutputGrouping bulmaOutputGrouping : listBulmaOutputGrouping) {
							Tuple2<Float, String> previousGPSPoint = null;
							Tuple2<Float, String> nextGPSPoint = null;
							List<Integer> pointsBetweenGPS = new LinkedList<Integer>();

							for (int i = 0; i < shapeLine.getListGeoPoint().size(); i++) {
								ShapePoint currentShapePoint = shapeLine.getListGeoPoint().get(i);
								String currentShapeSequence = currentShapePoint.getPointSequence();
								String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();

								String currentTimestamp;
								if (previousGPSPoint == null) {
									if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {
										
										currentTimestamp = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence).getTimestamp();
										previousGPSPoint = new Tuple2<Float, String>(currentShapePoint.getDistanceTraveled(), currentTimestamp);
										String buCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence).getBusCode();
										String problemCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence).getTripProblem();
										addOutput(shapeLine.getShapeId(), currentShapeSequence, currentDistanceTraveled, buCode, currentTimestamp, problemCode, listOutput);
										
									} else {
										addOutput(shapeLine.getShapeId(), currentShapeSequence, currentDistanceTraveled, "-", "-", "-", listOutput);
									}
								} else {
									
									if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {
										String busCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence).getBusCode();
										String problemCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence).getTripProblem();
										currentTimestamp = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence).getTimestamp();
										nextGPSPoint = new Tuple2<Float, String>(currentShapePoint.getDistanceTraveled(), currentTimestamp);


										generateOutputFromPointsInBetween(shapeLine.getShapeId(),previousGPSPoint, pointsBetweenGPS,
												nextGPSPoint, shapeLine.getListGeoPoint(), busCode, listOutput);

										
										addOutput(shapeLine.getShapeId(), currentShapeSequence, currentDistanceTraveled, busCode, currentTimestamp, problemCode, listOutput);

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
									String currentShapeSequence = shapeLine.getListGeoPoint().get(indexPointsInBetween).getPointSequence();
									String currentDistanceTraveled = shapeLine.getListGeoPoint().get(indexPointsInBetween).getDistanceTraveled().toString();
									addOutput(shapeLine.getShapeId(), currentShapeSequence, currentDistanceTraveled, "-", "-", "-", listOutput);

								}
							}
						}

						return listOutput.iterator();
					}

					private void addOutput(String shapeId,String currentShapeSequence, String distanceTraveled, String busCode, String currentTimestamp, String problemCode,
							List<String> listOutput) {
						String stopPointId = mapStopPoints.get(currentShapeSequence);
						if (stopPointId == null) {
							stopPointId = "-";
						}
						
						String problem;
						
						try {
							problem = Problem.getById(Integer.valueOf(problemCode));
						} catch (Exception e) {
							problem = "BETWEEN";
						}						
						
						listOutput.add(shapeId + "," + currentShapeSequence + "," + distanceTraveled + "," + busCode + ","+ currentTimestamp + "," + stopPointId + "," + problem);
					}

					private void generateOutputFromPointsInBetween(String shapeId, Tuple2<Float, String> previousGPSPoint,
							List<Integer> pointsBetweenGPS, Tuple2<Float, String> nextGPSPoint,
							List<ShapePoint> listGeoPointsShape, String busCode, List<String> listOutput) throws ParseException {

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
						for (Integer indexPointsInBetween : pointsBetweenGPS) {

							currentDistanceTraveled = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled()
									- previousDistanceTraveled;							
							generatedTimeDifference = (long) ((currentDistanceTraveled * time) / distanceTraveled);
							generatedTime = previousTime + generatedTimeDifference;
							generatedTimeString = getTimeString(generatedTime);
							sequence = listGeoPointsShape.get(indexPointsInBetween).getPointSequence();
							distance = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled().toString();
							addOutput(shapeId, sequence, distance,  busCode, generatedTimeString, "-", listOutput);

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

		rddInterpolation.saveAsTextFile("rddInterpolation");

	}
	
}
