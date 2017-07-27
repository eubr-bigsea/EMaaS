package BULMAStreaming;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import BULMADependences.GeoLine;
import BULMADependences.Problem;
import BULMADependences.ShapeLine;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;
import scala.Tuple3;

public class BULMAStreaming {

	private static Map<String, Map<String, ShapeLine>> mapShapeLines = new HashMap<>(); // key
																						// =
																						// route
	private static Map<String, Trip> mapCurrentTrip = new HashMap<>(); // key =
																		// bus
																		// code
																		// +
																		// route
	private static Map<String, List<ShapeLine>> mapTrueShapes = new HashMap<>();
	private static Map<String, Tuple2<Float, Float>> mapPreviousDistances = new HashMap<>();
	private static Map<String, List<GPSPoint>> mapRetrocessingPoints = new HashMap<>();
	private static Map<String, Integer> mapExtraThreshold = new HashMap<>();
	private static Map<String, List<Trip>> mapPreviousTrips = new HashMap<>();
	private static Map<String, List<String>> mapStopTimes = new HashMap<>();

	private static final double PERCENTAGE_DISTANCE = 0.09;
	private static final int THRESHOLD_RETROCESSING_POINTS = 1;
	private static final int THRESHOLD_OUTLIERS_POINTS = 2;
	private static final int THRESHOLD_DISTANCE_BETWEEN_POINTS = 50;
	private static final int NUMBER_SHAPES_IN_BASIC_CASE = 2;
	private static final String FILE_SEPARATOR = ",";

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 7) {
			System.err.println(
					"Usage: <shape file path> <Stop Time file path> <GPS files hostname> <GPS files port> <output path> "
							+ "<partitions number> <batch duration>");
			System.exit(1);
		}

		String pathFileShapes = args[0];
		String pathFileStopTime = args[1];
		String hostnameGPS = args[2];
		Integer portGPS = Integer.valueOf(args[3]);
		String pathOutput = args[4];
		int minPartitions = Integer.valueOf(args[5]);
		int batchDuration = Integer.valueOf(args[6]);

		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("BulmaRT");
		JavaStreamingContext context = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));

		Broadcast<Map<String, Map<String, ShapeLine>>> mapShapeLinesBroadcast = context.sparkContext()
				.broadcast(mapShapeLines);
		Broadcast<Map<String, Trip>> mapCurrentTripBroadcast = context.sparkContext().broadcast(mapCurrentTrip);
		Broadcast<Map<String, List<ShapeLine>>> mapTrueShapesBroadcast = context.sparkContext()
				.broadcast(mapTrueShapes);
		Broadcast<Map<String, Tuple2<Float, Float>>> mapPreviousDistancesBroadcast = context.sparkContext()
				.broadcast(mapPreviousDistances);
		Broadcast<Map<String, List<GPSPoint>>> mapRetrocessingPointsBroadcast = context.sparkContext()
				.broadcast(mapRetrocessingPoints);
		Broadcast<Map<String, Integer>> mapExtraThresholdBroadcast = context.sparkContext()
				.broadcast(mapExtraThreshold);
		Broadcast<Map<String, List<Trip>>> mapPreviousTripsBroadcast = context.sparkContext()
				.broadcast(mapPreviousTrips);
		Broadcast<Map<String, List<String>>> mapStopTimesBroadcast = context.sparkContext().broadcast(mapStopTimes);

		// =======================================================================

		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(pathFileStopTime));

			String sCurrentLine;
			br.readLine();
			while ((sCurrentLine = br.readLine()) != null) {
				String[] splittedLine = sCurrentLine.split(FILE_SEPARATOR);
				String timestamp = splittedLine[0];
				String shapeSequence = splittedLine[8];
				if (!mapStopTimesBroadcast.getValue().containsKey(shapeSequence)) {
					mapStopTimesBroadcast.getValue().put(shapeSequence, new LinkedList<>());
				}
				mapStopTimesBroadcast.getValue().get(shapeSequence).add(timestamp);
			}

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {

				if (br != null)
					br.close();

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		// =======================================================================

		/**
		 * Removes header (first line) from file
		 * 
		 * @return the file without the header
		 */
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

		/**
		 * Reads the shape file and puts it in JavaRDD<String>
		 */
		JavaRDD<String> shapeString = context.sparkContext().textFile(pathFileShapes, minPartitions)
				.mapPartitionsWithIndex(removeHeader, false);

		/**
		 * Creates shape point and groups by shape id
		 * 
		 * @return tuples of id and geo point set
		 */
		JavaPairRDD<String, Iterable<GeoPoint>> rddShapePointsPair = shapeString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(s);
						String shapeSequence = shapePoint.getPointSequence();

						List<String> listStopTimes = mapStopTimesBroadcast.getValue().get(shapeSequence);
						if (listStopTimes != null) {
							shapePoint.setListStopTimestamp(listStopTimes);
						}
						return new Tuple2<String, GeoPoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey();

		/**
		 * Creates a shape line with all points on the same route, puts in
		 * MapShapeLines and groups by shape route
		 * 
		 * @return tuples of route and geoline set
		 */
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

						Tuple2<Float, List<String>> previousStopPoint = null;
						Tuple2<Float, List<String>> nextStopPoint = null;
						List<Integer> pointsBetweenStops = new LinkedList<>();

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

							ShapePoint currentShapePoint = (ShapePoint) currentGeoPoint;
							if (previousStopPoint == null) {
								previousStopPoint = new Tuple2<Float, List<String>>(
										currentShapePoint.getDistanceTraveled(),
										currentShapePoint.getListStopTimestamp());
							} else {
								if (nextStopPoint == null) {

									if (currentShapePoint.getListStopTimestamp().isEmpty()) {
										pointsBetweenStops.add(i);
									} else {
										nextStopPoint = new Tuple2<Float, List<String>>(
												currentShapePoint.getDistanceTraveled(),
												currentShapePoint.getListStopTimestamp());


										// ===========================================
										// Code to populate listTimestampStop of all points based on listTimestampStop of stop points
										Float currentDistanceTraveled;
										for (Integer index : pointsBetweenStops) {
											currentDistanceTraveled = ((ShapePoint) listGeoPoint.get(index)).getDistanceTraveled();
											for (int indexListTimes = 0; indexListTimes < previousStopPoint._2.size(); indexListTimes++) {
												try {
													Float previousDistanceTraveled = previousStopPoint._1;
													long previousTime = getTimeLong(previousStopPoint._2.get(indexListTimes));
													Float nextDistanceTraveled = nextStopPoint._1;
													long nextTime = getTimeLong(nextStopPoint._2.get(indexListTimes));

													Float distanceTraveled = nextDistanceTraveled - previousDistanceTraveled;
													long time = nextTime - previousTime;

													long generatedTimeDifference = (long) ((currentDistanceTraveled * time)
															/ distanceTraveled);

													long generatedTime = previousTime + generatedTimeDifference;

													String generatedTimeString = getTimeString(generatedTime);
													((ShapePoint) listGeoPoint.get(index)).addTimestampStop(generatedTimeString);
												} catch (IndexOutOfBoundsException iobe) {
													System.err.println("Different sizes.");
												}
											}
										}
										// ===========================================
										
										
										previousStopPoint = nextStopPoint;
										nextStopPoint = null;
										pointsBetweenStops = new LinkedList<>();
									}
								}
							}

						}

						Coordinate[] array = new Coordinate[coordinates.size()];

						LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
						Float distanceTraveled = lastPoint.getDistanceTraveled();
						String route = lastPoint.getRoute();
						ShapeLine shapeLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								listGeoPoint, route, greaterDistance);

						int thresholdDistanceCurrentShape = (int) (shapeLine.getDistanceTraveled()
								/ (shapeLine.getListGeoPoints().size() * PERCENTAGE_DISTANCE));
						shapeLine.setThresholdDistance(thresholdDistanceCurrentShape);

						if (!mapShapeLinesBroadcast.getValue().containsKey(route)) {
							mapShapeLinesBroadcast.getValue().put(route, new HashMap<>());
						}

						mapShapeLinesBroadcast.getValue().get(route).put(pair._1, shapeLine);

						return new Tuple2<String, GeoLine>(lineBlockingKey, shapeLine);
					}

					private String getTimeString(long generatedTime) {
						Date date = new Date(generatedTime);
						DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
						return formatter.format(date);
					}
					
				});

		/**
		 * Receives the gps data stream by host name and port and put it in
		 * JavaDStream<String>
		 */
		JavaDStream<String> gpsStream = context.socketTextStream(hostnameGPS, portGPS);

		/**
		 * Creates gps point and put it in JavaDStream<GPSPoint>
		 */
		JavaDStream<GPSPoint> gpsPointsRDD = gpsStream.map(new Function<String, GPSPoint>() {

			@Override
			public GPSPoint call(String entry) throws Exception {
				return GPSPoint.createGPSPointWithId(entry);
			}
		});

		/**
		 * Calculates the best match between the current gps point and shape
		 * point
		 * 
		 * @return tuples with the gps point, the shape point matched and the
		 *         distance between them.
		 */
		JavaDStream<Tuple3<GPSPoint, ShapePoint, Float>> similarityOutput = gpsPointsRDD
				.flatMap(new FlatMapFunction<GPSPoint, Tuple3<GPSPoint, ShapePoint, Float>>() {

					@Override
					public Iterator<Tuple3<GPSPoint, ShapePoint, Float>> call(GPSPoint currentGPSPoint)
							throws Exception {

						List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput = new ArrayList<>();
						String route = currentGPSPoint.getLineCode();
						String keyMaps = currentGPSPoint.getBusCode() + route;
						Trip trip = mapCurrentTripBroadcast.getValue().get(keyMaps);
						if (trip == null) {
							trip = new Trip();
						}

						populateTrueShapes(route, currentGPSPoint);
						List<ShapeLine> listTrueShapes = mapTrueShapesBroadcast.getValue().get(keyMaps);

						if (listTrueShapes != null) {
							if (trip.hasFoundInitialPoint()) {
								if (trip.getNumberOfOutliersInSequence() >= THRESHOLD_OUTLIERS_POINTS
										&& listTrueShapes.size() > NUMBER_SHAPES_IN_BASIC_CASE) {
									findRightShape(listTrueShapes, trip);
								}
								findEndPoint(trip, currentGPSPoint, listOutput, listTrueShapes, keyMaps);

							}
							if (!trip.hasFoundInitialPoint()) {
								findFirstPoint(listTrueShapes, currentGPSPoint, trip, listOutput, keyMaps);
							}
						} else {
							currentGPSPoint.setProblem(Problem.NO_SHAPE.getCode());
							listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint, null,
									Float.valueOf(currentGPSPoint.getProblem())));
						}

						return listOutput.iterator();
					}

					/**
					 * Selects the case of the route (circular or
					 * complementary), look for shapes that are of the the same
					 * route and put them in MapTrueShapes
					 * 
					 * @param route
					 * @param possibleFirstPoint
					 */
					private void populateTrueShapes(String route, GPSPoint possibleFirstPoint) {

						String currentBusCodeAndRoute = possibleFirstPoint.getBusCode() + route;
						if (route.equals("REC")) {
							mapTrueShapesBroadcast.getValue().put(currentBusCodeAndRoute, null);

						} else {
							if (mapTrueShapesBroadcast.getValue().get(currentBusCodeAndRoute) == null) {

								List<ShapeLine> listMatchedShapes = new ArrayList<>();
								Map<String, ShapeLine> mapPossibleShapeLines = mapShapeLinesBroadcast.getValue()
										.get(route);

								if (mapPossibleShapeLines != null
										&& mapPossibleShapeLines.size() >= NUMBER_SHAPES_IN_BASIC_CASE) {
									ShapeLine bestShape = null;
									Float smallerDistanceTraveled = Float.MAX_VALUE;
									float sumGreaterDistancePoints = 0;

									for (Entry<String, ShapeLine> entry : mapPossibleShapeLines.entrySet()) {
										ShapeLine shapeLine = entry.getValue();
										sumGreaterDistancePoints += shapeLine.getGreaterDistancePoints();
										ShapePoint closestPoint = getClosestShapePoint(possibleFirstPoint,
												shapeLine)._1;

										if (closestPoint != null
												&& closestPoint.getDistanceTraveled() <= smallerDistanceTraveled) {
											smallerDistanceTraveled = closestPoint.getDistanceTraveled();
											bestShape = shapeLine;
										}
									}

									float thresholdShapesSequence = sumGreaterDistancePoints
											/ mapPossibleShapeLines.size();

									Point firstPoint = bestShape.getLine().getStartPoint();
									Point endPoint = bestShape.getLine().getEndPoint();

									if (GPSPoint.getDistanceInMeters(firstPoint, endPoint) <= thresholdShapesSequence) {
										listMatchedShapes.add(bestShape);
									} else {
										listMatchedShapes.addAll(mapPossibleShapeLines.values());
									}

								} else if (mapPossibleShapeLines != null && mapPossibleShapeLines.size() == 1) {
									listMatchedShapes.addAll(mapPossibleShapeLines.values());
								}

								mapTrueShapesBroadcast.getValue().put(currentBusCodeAndRoute, listMatchedShapes);
							}
						}
					}

					/**
					 * Finds the right shape when THRESHOLD_OUTLIERS_POINTS
					 * points is above THRESHOLD_DISTANCE_BETWEEN_POINTS for the
					 * selected shape.
					 * 
					 * @param listTrueShapes
					 *            shapes list of the route
					 * @param trip
					 *            current trip with outliers points
					 */
					private void findRightShape(List<ShapeLine> listTrueShapes, Trip trip) {

						List<ShapePoint> matchedShapesList;

						Tuple2<ShapePoint, Float> closestPoint;
						for (ShapeLine currentShapeLine : listTrueShapes) {
							matchedShapesList = new ArrayList<>();
							for (GPSPoint outlierPoint : trip.getOutliersInSequence()) {

								closestPoint = getClosestShapePoint(outlierPoint, currentShapeLine);
								if (closestPoint._2 <= THRESHOLD_DISTANCE_BETWEEN_POINTS) {
									matchedShapesList.add(closestPoint._1);
								}
							}

							Float previousDistanceTraveled = Float.MAX_VALUE;
							for (int i = 0; i < matchedShapesList.size(); i++) {
								if (matchedShapesList.get(i).getDistanceTraveled() > previousDistanceTraveled) {
									previousDistanceTraveled = null;
									break;
								}

								previousDistanceTraveled = matchedShapesList.get(i).getDistanceTraveled();
							}

							if (!matchedShapesList.isEmpty() && previousDistanceTraveled != null) {
								trip.setShapeMatched(currentShapeLine);
								trip.cleanOutliersInSequenceList();
								break;
							}
						}
					}

					/**
					 * Find the (possible) first point of the trip and update
					 * MapCurrentTrip and MapPreviousDistances
					 * 
					 * @param listTrueShapes
					 *            shapes list of the route
					 * @param currentGPSPoint
					 * @param trip
					 * @param listOutput
					 * @param keyMaps
					 *            bus code of the currentGPSPoint + route
					 */
					private void findFirstPoint(List<ShapeLine> listTrueShapes, GPSPoint currentGPSPoint, Trip trip,
							List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput, String keyMaps) {

						Tuple3<ShapeLine, ShapePoint, Float> shapeMatchedAndClosestPoint = getShapeMatchedAndClosestPoint(
								currentGPSPoint, listTrueShapes);

						if (shapeMatchedAndClosestPoint == null) { // when doesn't have shape with the same route
							listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint, null,
									Float.valueOf(currentGPSPoint.getProblem())));

						} else {
							trip.setInitialPoint(currentGPSPoint);
							trip.setShapeMatched(shapeMatchedAndClosestPoint._1());
							trip.addPointToPath(currentGPSPoint, shapeMatchedAndClosestPoint._2(),
									shapeMatchedAndClosestPoint._3());
							listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint,
									shapeMatchedAndClosestPoint._2(), shapeMatchedAndClosestPoint._3()));
							mapCurrentTripBroadcast.getValue().put(keyMaps, trip);
							mapPreviousDistancesBroadcast.getValue().put(keyMaps,
									new Tuple2<Float, Float>(shapeMatchedAndClosestPoint._2().getDistanceTraveled(),
											shapeMatchedAndClosestPoint._3()));
							mapCurrentTripBroadcast.getValue().put(keyMaps, trip);
						}
					}

					/**
					 * Gets the closest shape point, with the shortest distance
					 * traveled, and its shape matched with the gps point
					 * 
					 * @param gpsPoint
					 * @param listTrueShapes
					 *            shapes list of the route
					 * @return tuple with the shape matched, closest shape point
					 *         e the distance between it and the gps point
					 */
					private Tuple3<ShapeLine, ShapePoint, Float> getShapeMatchedAndClosestPoint(GPSPoint gpsPoint,
							List<ShapeLine> listTrueShapes) {

						if (listTrueShapes == null || listTrueShapes.size() == 0) {
							gpsPoint.setProblem(Problem.NO_SHAPE.getCode());
							return null;
						}

						Float smallerDistanceTraveled = null;
						ShapePoint shapePointMatched = null;
						Float distanceInMeters = null;
						ShapeLine shapeLineMatched = null;

						Tuple2<ShapePoint, Float> closestPoint;
						for (ShapeLine currentShapeLine : listTrueShapes) {
							closestPoint = getClosestShapePoint(gpsPoint, currentShapeLine);
							if (smallerDistanceTraveled == null
									|| (closestPoint._1.getDistanceTraveled() < smallerDistanceTraveled
											&& (closestPoint._2 <= THRESHOLD_DISTANCE_BETWEEN_POINTS
													|| closestPoint._2 < distanceInMeters))) {

								smallerDistanceTraveled = closestPoint._1.getDistanceTraveled();
								shapePointMatched = closestPoint._1;
								distanceInMeters = closestPoint._2;
								shapeLineMatched = currentShapeLine;
							}
						}

						if (distanceInMeters > THRESHOLD_DISTANCE_BETWEEN_POINTS) {
							gpsPoint.setProblem(Problem.OUTLIER_POINT.getCode());
						}

						return new Tuple3<ShapeLine, ShapePoint, Float>(shapeLineMatched, shapePointMatched,
								distanceInMeters);
					}

					/**
					 * Gets the closest shape point of the gps point
					 * 
					 * @param gpsPoint
					 * @param shapeLine
					 *            the shape line matched with the trip of the
					 *            gps point
					 * @return tuple with the closest shape point and the
					 *         distance between it and the gps point
					 */
					private Tuple2<ShapePoint, Float> getClosestShapePoint(GPSPoint gpsPoint, ShapeLine shapeLine) {

						Float smallerDistance = Float.MAX_VALUE;
						ShapePoint closestShapePoint = null;

						Float currentDistance;
						for (GeoPoint shapePoint : shapeLine.getListGeoPoints()) {
							currentDistance = GeoPoint.getDistanceInMeters(gpsPoint, shapePoint);
							if (currentDistance < smallerDistance) {
								smallerDistance = currentDistance;
								closestShapePoint = (ShapePoint) shapePoint;
							}
						}

						return new Tuple2<ShapePoint, Float>(closestShapePoint, smallerDistance);
					}

					/**
					 * Finds the end point of the trip. Puts current gps point
					 * in MapRetrocessingPoints, if its closest shape point has
					 * smaller distance traveled than the last one gps point.
					 * Puts current gps point in ListOutlierInSequence, if its
					 * distance to closest shape point is above
					 * THRESHOLD_DISTANCE_BETWEEN_POINTS.
					 * 
					 * @param trip
					 * @param currentGPSPoint
					 * @param listOutput
					 * @param listTrueShapes
					 * @param keyMaps
					 */
					private void findEndPoint(Trip trip, GPSPoint currentGPSPoint,
							List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput, List<ShapeLine> listTrueShapes,
							String keyMaps) {

						Tuple2<Float, Float> previousDistance = mapPreviousDistancesBroadcast.getValue().get(keyMaps);
						Float previousDistanceTraveled = previousDistance._1;
						Float previousDistanceInMeters = previousDistance._2;
						List<GPSPoint> listRetrocessingPoints = mapRetrocessingPointsBroadcast.getValue().get(keyMaps);
						Integer extraThreshold = mapExtraThresholdBroadcast.getValue().get(keyMaps);
						Tuple2<ShapePoint, Float> currentClosestShapePoint = getClosestShapePoint(currentGPSPoint,
								trip.getShapeMatched());

						if (currentClosestShapePoint._1.getDistanceTraveled() <= previousDistanceTraveled) {

							if (extraThreshold == null) {
								extraThreshold = 0;
							}

							if (currentClosestShapePoint._1.getDistanceTraveled().equals(previousDistanceTraveled)
									&& (currentClosestShapePoint._2.equals(previousDistanceInMeters)
											|| currentClosestShapePoint._2 < previousDistanceInMeters)) {
								extraThreshold += 1; // conditions to add equals points in the list without compromise the algorithm
								mapExtraThresholdBroadcast.getValue().put(keyMaps, extraThreshold);
							}
							if (listRetrocessingPoints == null) {
								listRetrocessingPoints = new ArrayList<>();
							}

							if (listRetrocessingPoints.size() < THRESHOLD_RETROCESSING_POINTS + extraThreshold) {
								listRetrocessingPoints.add(currentGPSPoint);
								mapRetrocessingPointsBroadcast.getValue().put(keyMaps, listRetrocessingPoints);
								mapPreviousDistancesBroadcast.getValue().put(keyMaps,
										new Tuple2<Float, Float>(currentClosestShapePoint._1().getDistanceTraveled(),
												currentClosestShapePoint._2));

							} else { // checking which of the retrocessing points is end point

								List<Trip> previousTrips = mapPreviousTripsBroadcast.getValue().get(keyMaps);
								if (previousTrips == null) {
									previousTrips = new ArrayList<>();
								}
								previousTrips.add(trip);
								mapPreviousTripsBroadcast.getValue().put(keyMaps, previousTrips);

								trip = new Trip();

								GPSPoint currentRetrocessingPoint;
								for (int i = 0; i < listRetrocessingPoints.size(); i++) {
									currentRetrocessingPoint = listRetrocessingPoints.get(i);
									if (i == 0) {
										Tuple3<ShapeLine, ShapePoint, Float> shapeMatchedAndClosestPoint = getShapeMatchedAndClosestPoint(
												currentRetrocessingPoint, listTrueShapes);
										if (shapeMatchedAndClosestPoint != null) {
											trip.setInitialPoint(currentRetrocessingPoint);
											trip.setShapeMatched(shapeMatchedAndClosestPoint._1());
											trip.addPointToPath(currentRetrocessingPoint,
													shapeMatchedAndClosestPoint._2(), shapeMatchedAndClosestPoint._3());
											listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(
													listRetrocessingPoints.get(0), shapeMatchedAndClosestPoint._2(),
													shapeMatchedAndClosestPoint._3()));
										}

									} else {
										Tuple2<ShapePoint, Float> closestShapePointRetrocessing = getClosestShapePoint(
												currentRetrocessingPoint, trip.getShapeMatched());
										if (closestShapePointRetrocessing._2 > THRESHOLD_DISTANCE_BETWEEN_POINTS) {
											currentRetrocessingPoint.setProblem(Problem.OUTLIER_POINT.getCode());
											trip.addOutlierInSequence(currentGPSPoint);
										} else {
											trip.cleanOutliersInSequenceList();
										}
										trip.addPointToPath(currentRetrocessingPoint, closestShapePointRetrocessing._1,
												closestShapePointRetrocessing._2);
										listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentRetrocessingPoint,
												closestShapePointRetrocessing._1(), closestShapePointRetrocessing._2));
									}
								}

								// clean maps
								mapRetrocessingPointsBroadcast.getValue().put(keyMaps, new ArrayList<>());
								mapExtraThresholdBroadcast.getValue().put(keyMaps, 0);

								currentClosestShapePoint = getClosestShapePoint(currentGPSPoint,
										trip.getShapeMatched());
								mapPreviousDistancesBroadcast.getValue().put(keyMaps,
										new Tuple2<Float, Float>(currentClosestShapePoint._1().getDistanceTraveled(),
												currentClosestShapePoint._2()));

								if (currentClosestShapePoint._2 > THRESHOLD_DISTANCE_BETWEEN_POINTS) {
									currentGPSPoint.setProblem(Problem.OUTLIER_POINT.getCode());
									trip.addOutlierInSequence(currentGPSPoint);
								} else {
									trip.cleanOutliersInSequenceList();
								}

								trip.addPointToPath(currentGPSPoint, currentClosestShapePoint._1,
										currentClosestShapePoint._2);
								listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint,
										currentClosestShapePoint._1(), currentClosestShapePoint._2));
								mapCurrentTripBroadcast.getValue().put(keyMaps, trip);
							}
						} else { // the current gps point continues on the same
									// trip

							if (listRetrocessingPoints != null) {
								for (GPSPoint retrocessingPoint : listRetrocessingPoints) {
									Tuple2<ShapePoint, Float> closestPointRetrocessing = getClosestShapePoint(
											retrocessingPoint, trip.getShapeMatched());
									if (closestPointRetrocessing._2 > THRESHOLD_DISTANCE_BETWEEN_POINTS) {
										retrocessingPoint.setProblem(Problem.OUTLIER_POINT.getCode());
										trip.addOutlierInSequence(currentGPSPoint);
									} else {
										trip.cleanOutliersInSequenceList();
									}
									trip.addPointToPath(retrocessingPoint, closestPointRetrocessing._1,
											closestPointRetrocessing._2);
									listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(retrocessingPoint,
											closestPointRetrocessing._1(), closestPointRetrocessing._2));
								}
							}

							if (trip.getNumberOfOutliersInSequence() >= THRESHOLD_OUTLIERS_POINTS
									&& listTrueShapes.size() > NUMBER_SHAPES_IN_BASIC_CASE) {
								findRightShape(listTrueShapes, trip);
							}

							// clean maps
							mapExtraThresholdBroadcast.getValue().put(keyMaps, 0);
							mapRetrocessingPointsBroadcast.getValue().put(keyMaps, new ArrayList<>());

							if (currentClosestShapePoint._2 > THRESHOLD_DISTANCE_BETWEEN_POINTS) {
								currentGPSPoint.setProblem(Problem.OUTLIER_POINT.getCode());
								trip.addOutlierInSequence(currentGPSPoint);
							} else {
								trip.cleanOutliersInSequenceList();
							}

							trip.addPointToPath(currentGPSPoint, currentClosestShapePoint._1,
									currentClosestShapePoint._2);
							listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint,
									currentClosestShapePoint._1(), currentClosestShapePoint._2));
							mapCurrentTripBroadcast.getValue().put(keyMaps, trip);
							mapPreviousDistancesBroadcast.getValue().put(keyMaps,
									new Tuple2<Float, Float>(currentClosestShapePoint._1().getDistanceTraveled(),
											currentClosestShapePoint._2()));
						}
					}
				});

		/**
		 * Formats the output.
		 */
		JavaDStream<String> rddOutput = similarityOutput
				.map(new Function<Tuple3<GPSPoint, ShapePoint, Float>, String>() {

					private final String[] SITUATIONS = { "NO DATA", "LATE", "IN TIME", "IN ADVANCE" };
					private final long DELAY_TOLERANCE_TIME = 300000L; // 5minutes
					
					@Override
					public String call(Tuple3<GPSPoint, ShapePoint, Float> t) throws Exception {
						String stringOutput = "";

						GPSPoint gpsPoint = t._1();
						ShapePoint shapePoint = t._2();

						if (shapePoint != null) {
							stringOutput += shapePoint.getRoute() + FILE_SEPARATOR;
							stringOutput += shapePoint.getLatitude() + FILE_SEPARATOR;
							stringOutput += shapePoint.getLongitude() + FILE_SEPARATOR;
							stringOutput += shapePoint.getDistanceTraveled() + FILE_SEPARATOR;
						} else {
							stringOutput += "-" + FILE_SEPARATOR;
							stringOutput += "-" + FILE_SEPARATOR;
							stringOutput += "-" + FILE_SEPARATOR;
							stringOutput += "-" + FILE_SEPARATOR;
						}
						stringOutput += gpsPoint.getBusCode() + FILE_SEPARATOR;
						stringOutput += gpsPoint.getGpsId() + FILE_SEPARATOR;
						stringOutput += gpsPoint.getLatitude() + FILE_SEPARATOR;
						stringOutput += gpsPoint.getLongitude() + FILE_SEPARATOR;
						stringOutput += gpsPoint.getProblem() + FILE_SEPARATOR;
						stringOutput += t._3() + FILE_SEPARATOR;

						if (shapePoint != null && !shapePoint.getListStopTimestamp().isEmpty()) {
							
							Long smallerDifference = null;
							String expectedTime = null;
							
							// run the listStopTimestamp of the shape matched to find the hour correspondent with the GPS point
							// In this case the hour that most approximate to the current hour of that point
							for (String timestampString : shapePoint.getListStopTimestamp()) {
								
								long timeDifference = getTimeLong(timestampString) - gpsPoint.getTime();
								if (smallerDifference == null || Math.abs(smallerDifference) > Math.abs(timeDifference)){
									smallerDifference = timeDifference;
									expectedTime = timestampString;
								}
							}
							
							stringOutput += expectedTime + FILE_SEPARATOR;
							stringOutput += gpsPoint.getTimeStamp() + FILE_SEPARATOR;
							
							if (Math.abs(smallerDifference)  > DELAY_TOLERANCE_TIME) { // if the abs value of the difference is within the threshold
								if (smallerDifference > 0) { // the bus is in advance
									stringOutput +=  SITUATIONS[3];
								} else { // the bus is late
									stringOutput += SITUATIONS[1];
								}
								
							} else {// the bus is in time
								stringOutput += SITUATIONS[2]; 
							}
													
						} else { // There is no data to compare with current gps time
							stringOutput += "-" + FILE_SEPARATOR;
							stringOutput += gpsPoint.getTimeStamp() + FILE_SEPARATOR;
							stringOutput += SITUATIONS[0];
						}
						
						return stringOutput;
					}
				});

		// Saves the shape output e gps outputs
		rddShapeLinePair.saveAsTextFile(pathOutput + "Shape");
		rddOutput.dstream().saveAsTextFiles(pathOutput, "GPS");

		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static long getTimeLong(String timestamp) throws ParseException {
		SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
		return parser.parse(timestamp).getTime();
	}

}