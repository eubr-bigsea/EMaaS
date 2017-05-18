package IncrementalLineMatching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import IncrementalLineDependencies.Trip;
import LineDependencies.GeoLine;
import LineDependencies.Problem;
import LineDependencies.ShapeLine;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;
import scala.Tuple3;

public class IncrementalBULMA {

	// MAP<ROUTE, LIST<TUPLE2<INITIAL_POINT, SHAPE>>
	private static Map<String, Map<String, ShapeLine>> mapRouteShapeLine = new HashMap<>();
	// MAP<BUS_CODE, TRIP>
	private static Map<String, Trip> mapBusCodeTrip = new HashMap<>();

	private static Map<String, Tuple2<Float, List<GPSPoint>>> mapDistances = new HashMap<>();
	
	private static Map<String, Tuple2<Float, GPSPoint>> mapDistancesToEndPoints = new HashMap<>();
	
	private static final int THRESHOLD = 40;

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 6) {
			System.err.println("Usage: <shape file path> <GPS files hostname> <GPS files port> <output path> "
					+ "<partitions number> <batch duration");
			System.exit(1);
		}

		String pathFileShapes = args[0];
		String hostnameGPS = args[1];
		Integer portGPS = Integer.valueOf(args[2]);
		String pathOutput = args[3];
		int minPartitions = Integer.valueOf(args[4]);
		int batchDuration = Integer.valueOf(args[5]);

		if (pathFileShapes.equals(pathOutput)) {
			System.out.println("The output directory should not be the same as the Shape file directory.");
		}

		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("IncrementalBulma");
		JavaStreamingContext context = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));

		Function2<Integer, Iterator<String>, Iterator<String>> removeHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {
			@Override
			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				if (index == 0 && iterator.hasNext()) {
					iterator.next();
					return iterator;
				} else {
					return iterator;
				}
			}
		};

		JavaRDD<String> shapeString = context.sparkContext().textFile(pathFileShapes, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<GeoPoint>> rddShapePointsPair = shapeString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(s);
						return new Tuple2<String, GeoPoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey();

		JavaPairRDD<String, GeoLine> rddShapeLinePair = rddShapePointsPair
				.mapToPair(new PairFunction<Tuple2<String, Iterable<GeoPoint>>, String, GeoLine>() {

					@SuppressWarnings("deprecation")
					GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();

					@Override
					public Tuple2<String, GeoLine> call(Tuple2<String, Iterable<GeoPoint>> pair) throws Exception {

						List<Coordinate> coordinates = new ArrayList<>();
						Double latitude;
						Double longitude;
						ShapePoint lastPoint = null;
						String lineBlockingKey = null;
						float greaterDistance = 0;

						List<GeoPoint> listGeoPoint = Lists.newArrayList(pair._2);
						for (int i = 0; i < listGeoPoint.size(); i++) {
							GeoPoint currentGeoPoint = listGeoPoint.get(i);
							if (i < listGeoPoint.size() - 1) {
								float currentDistance = GeoPoint.getDistanceInMeters(currentGeoPoint,
										listGeoPoint.get(i + 1));
								if (currentDistance > greaterDistance) {
									greaterDistance = currentDistance;
								}
							}

							latitude = Double.valueOf(currentGeoPoint.getLatitude());
							longitude = Double.valueOf(currentGeoPoint.getLongitude());
							coordinates.add(new Coordinate(latitude, longitude));
							lastPoint = (ShapePoint) currentGeoPoint;

							if (lineBlockingKey == null) {
								lineBlockingKey = ((ShapePoint) currentGeoPoint).getRoute();
							}
						}

						Coordinate[] array = new Coordinate[coordinates.size()];

						LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
						String distanceTraveled = lastPoint.getDistanceTraveled();
						String route = lastPoint.getRoute();
						ShapeLine geoLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								listGeoPoint, route, greaterDistance);

						if (!mapRouteShapeLine.containsKey(route)) {
							mapRouteShapeLine.put(route, new HashMap<>());
						}
						Map<String, ShapeLine> map = mapRouteShapeLine.get(route);
						map.put(pair._1, geoLine);

						mapRouteShapeLine.get(route).putAll(map);
						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});

		JavaDStream<String> gpsStream = context.socketTextStream(hostnameGPS, portGPS);

		JavaDStream<Tuple3<GPSPoint, ShapePoint, Float>> similarityOutput = gpsStream
				.flatMap(new FlatMapFunction<String, Tuple3<GPSPoint, ShapePoint, Float>>() {

					@Override
					public Iterator<Tuple3<GPSPoint, ShapePoint, Float>> call(String entry) throws Exception {

						List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput = new ArrayList<>();
						GPSPoint currentGPSPoint = GPSPoint.createGPSPointWithId(entry);
						String route = currentGPSPoint.getLineCode();

//						if (route.equals("207") && currentGPSPoint.getBusCode().equals("BC301")) {

							Trip trip = mapBusCodeTrip.get(currentGPSPoint.getBusCode());

							if (trip == null) {
								trip = new Trip();
							}

							if (trip.hasFoundInitialPoint()) {
								
								ShapeLine shapeLine = mapRouteShapeLine.get(route).get(trip.getShapeMatching().getId());
								ShapePoint startPointShape = (ShapePoint) shapeLine.getListGeoPoints().get(0);
								ShapePoint endPointShape = (ShapePoint) shapeLine.getListGeoPoints().get(shapeLine.getListGeoPoints().size() -1);
								float currentDistanceToStartPoint = GeoPoint.getDistanceInMeters(currentGPSPoint,
										startPointShape);
								float currentDistanceToEndPoint = GeoPoint.getDistanceInMeters(currentGPSPoint,
										endPointShape);
								
								//TODO To find the end point and clean the list to begin a new trip
								if (!trip.isNearToEndPoint() && currentDistanceToStartPoint > THRESHOLD && currentDistanceToEndPoint <= THRESHOLD) {
									trip.setNearToEndPoint(true);
								}
								
								if (trip.isNearToEndPoint()) {
									if (mapDistancesToEndPoints.containsKey(currentGPSPoint.getBusCode())) {
										if (mapDistancesToEndPoints.get(currentGPSPoint.getBusCode())._1 < currentDistanceToEndPoint) {
											mapDistancesToEndPoints.put(currentGPSPoint.getBusCode(), new Tuple2<Float, GPSPoint>(currentDistanceToEndPoint, currentGPSPoint));
										
										} else {
											trip.setEndPoint(mapDistancesToEndPoints.get(currentGPSPoint.getBusCode())._2);
											
											mapDistances = new HashMap<>();
											mapBusCodeTrip = new HashMap<>(); //TODO clean value just of the current key
											trip = new Trip();
											trip.setInitialPoint(currentGPSPoint);
										}
										
									} else {
										mapDistancesToEndPoints.put(currentGPSPoint.getBusCode(), new Tuple2<Float, GPSPoint>(currentDistanceToEndPoint, currentGPSPoint));
									}	
								} 
								
								addClosestPoint(currentGPSPoint, shapeLine, trip, listOutput);
								mapBusCodeTrip.put(currentGPSPoint.getBusCode(), trip);

							} else {
								
								if (mapRouteShapeLine.containsKey(route)) {

									for (ShapeLine shapeLine : mapRouteShapeLine.get(route).values()) {
										Tuple2<Float, List<GPSPoint>> closestPointToFirstPointShape;
										Float smallerDistanceToFirstPoint = null;
										closestPointToFirstPointShape = mapDistances
												.get(currentGPSPoint.getBusCode() + shapeLine.getId());

										Tuple2<Float, List<GPSPoint>> listOutliersBeforeAndDistance = mapDistances
												.get(currentGPSPoint.getBusCode() + shapeLine.getId());
										if (listOutliersBeforeAndDistance == null) {
											listOutliersBeforeAndDistance = new Tuple2<Float, List<GPSPoint>>(null,
													new ArrayList<GPSPoint>());
										}
										List<GPSPoint> listOutliersBefore = listOutliersBeforeAndDistance._2;
										
										if (closestPointToFirstPointShape != null) {
											smallerDistanceToFirstPoint = closestPointToFirstPointShape._1;
										}

										ShapePoint startPointShape = (ShapePoint) shapeLine.getListGeoPoints().get(0);

										float currentDistance = GeoPoint.getDistanceInMeters(currentGPSPoint,
												startPointShape);
										if (smallerDistanceToFirstPoint == null
												|| currentDistance < smallerDistanceToFirstPoint) {
											smallerDistanceToFirstPoint = currentDistance;
											
											listOutliersBefore.add(currentGPSPoint);
											mapDistances.put(currentGPSPoint.getBusCode() + shapeLine.getId(),
													new Tuple2<Float, List<GPSPoint>>(smallerDistanceToFirstPoint,
															listOutliersBefore));
											
										} else if (smallerDistanceToFirstPoint <= THRESHOLD) { // TODO Check threshold
											// TODO Check which shape it chose
											trip.setHasFoundInitialPoint(true);
											List<GPSPoint> pointsBefore = mapDistances
													.get(currentGPSPoint.getBusCode() + shapeLine.getId())._2;
											for (int i = 0; i < pointsBefore.size() - 1; i++) {
												trip.addOutlierBefore(pointsBefore.get(i));
												listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(pointsBefore.get(i), null, null));
											}
											GPSPoint initialGPSPoint = pointsBefore.get(pointsBefore.size() - 1);
											trip.setInitialPoint(initialGPSPoint);
											trip.setShapeMatching(shapeLine);

											addClosestPoint(initialGPSPoint, shapeLine, trip, listOutput);
											addClosestPoint(currentGPSPoint, shapeLine, trip, listOutput);
											trip.addPointToPath(initialGPSPoint, startPointShape,
													smallerDistanceToFirstPoint);
											trip.addPointToPath(currentGPSPoint, startPointShape,
													smallerDistanceToFirstPoint);
											mapBusCodeTrip.put(currentGPSPoint.getBusCode(), trip);
											break;
											
										} else { // if currentPoint has a distance smaller than previous distance but it is not the initial point according to the threshold
											listOutliersBefore.add(currentGPSPoint);
											mapDistances.put(currentGPSPoint.getBusCode() + shapeLine.getId(),
													new Tuple2<Float, List<GPSPoint>>(smallerDistanceToFirstPoint,
															listOutliersBefore));
										}
									}
								} else { // when doesn't have shape in the dataset
									listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint, null, Float.valueOf(Problem.NO_SHAPE.getCode())));
								}
							}
//						}

						return listOutput.iterator();
					}

					private void addClosestPoint(GPSPoint gpsPoint, ShapeLine shapeLine, Trip trip,
							List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput) throws Exception {
						Float smallerDistance = null;
						Float currentDistance;
						for (GeoPoint shapePoint : shapeLine.getListGeoPoints()) {
							currentDistance = GeoPoint.getDistanceInMeters(gpsPoint, shapePoint);
							if (smallerDistance == null || currentDistance < smallerDistance) {
								smallerDistance = currentDistance;
								gpsPoint.setClosestPoint(shapePoint);
							}
						}
						trip.addPointToPath(gpsPoint, gpsPoint.getClosestPoint(), smallerDistance);
						listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(gpsPoint, gpsPoint.getClosestPoint(),
								smallerDistance));
					}
				});

		rddShapeLinePair.saveAsTextFile(pathOutput + "Shape");
		similarityOutput.dstream().saveAsTextFiles(pathOutput, "GPS");

		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}