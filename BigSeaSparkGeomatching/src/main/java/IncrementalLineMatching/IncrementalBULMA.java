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
import com.vividsolutions.jts.geom.Point;
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
	private static Map<String, List<Tuple2<Point, GeoLine>>> mapRouteShapeLine = new HashMap<>();
	// MAP<BUS_CODE, TRIP>
	private static Map<String, Trip> mapBusCodeTrip = new HashMap<>();

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 6) {
			System.err.println("Usage: <shape file path> <GPS files hostname> <GPS files port> <output path> "
					+ "<partitions number> <batch duration");
			System.exit(1);
		}

		String pathFileShapes = args[0];
		String GPSHostname = args[1];
		Integer GPSPort = Integer.valueOf(args[2]);
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
						GeoLine geoLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								listGeoPoint, route, greaterDistance);

						Point initialPoint = lineString.getStartPoint();
						Tuple2<Point, GeoLine> initialPointShapeLine = new Tuple2<Point, GeoLine>(initialPoint,
								geoLine);
						if (mapRouteShapeLine.containsKey(route)) {
							mapRouteShapeLine.get(route).add(initialPointShapeLine);
						} else {
							List<Tuple2<Point, GeoLine>> listInitialPointShapeLine = new ArrayList<>();
							listInitialPointShapeLine.add(initialPointShapeLine);
							mapRouteShapeLine.put(route, listInitialPointShapeLine);
						}
						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});

		JavaDStream<String> gpsStream = context.socketTextStream(GPSHostname, GPSPort);

		JavaDStream<Tuple3<GPSPoint, Point, Float>> similarityOutput = gpsStream
				.flatMap(new FlatMapFunction<String, Tuple3<GPSPoint, Point, Float>>() {

					@Override
					public Iterator<Tuple3<GPSPoint, Point, Float>> call(String entry) throws Exception {

						List<Tuple3<GPSPoint, Point, Float>> listOutput = new ArrayList<>();
						GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(entry);
						String route = gpsPoint.getLineCode();

						Trip trip = mapBusCodeTrip.get(gpsPoint.getBusCode());

						if (trip == null) {
							trip = new Trip();
						}

						if (trip.hasFoundInitialPoint()) {
							// TODO put points after initial point
							listOutput.add(new Tuple3<GPSPoint, Point, Float>(gpsPoint, null, null));

						} else {

							if (mapRouteShapeLine.containsKey(route)) {
								//TODO check duplicate output
								for (Tuple2<Point, GeoLine> initialPointShapeLine : mapRouteShapeLine.get(route)) {
									Point initialPointShape = initialPointShapeLine._1;
									ShapeLine shapeLine = ((ShapeLine) initialPointShapeLine._2);
									GPSPoint initialPointTrip = trip.getInitialPoint();
									float distance = GeoPoint.getDistanceInMeters(
											Double.valueOf(gpsPoint.getLatitude()),
											Double.valueOf(gpsPoint.getLongitude()), initialPointShape.getX(),
											initialPointShape.getY());

									if (initialPointTrip == null || distance <= trip.getDistanceToInitialPoint()) {
										trip.addOutlierBefore(initialPointTrip);
										trip.setInitialPoint(gpsPoint);
										trip.setShapeMatching(shapeLine);

									} else { // when found initial point
										trip.setHasFoundInitialPoint(true);
										for (GPSPoint outlierBefore : trip.getOutliersBefore()) {
											listOutput.add(new Tuple3<GPSPoint, Point, Float>(outlierBefore, null,
													Float.valueOf(Problem.OUTLIER_POINT.getCode())));
										}

										trip.getPath().put(initialPointTrip,
												new Tuple2<Point, Float>(initialPointShape, distance));
										listOutput.add(new Tuple3<GPSPoint, Point, Float>(initialPointTrip,
												initialPointShape, distance));
									}
								}

								mapBusCodeTrip.put(gpsPoint.getBusCode(), trip);

							} else { // when doesn't have shape in the dataset
								listOutput.add(new Tuple3<GPSPoint, Point, Float>(gpsPoint, null,
										Float.valueOf(Problem.NO_SHAPE.getCode())));
							}
						}

						return listOutput.iterator();
					}
				});

		rddShapeLinePair.saveAsTextFile(pathOutput + "shape");
		similarityOutput.dstream().saveAsTextFiles(pathOutput, "gps");

		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}