package LineMatching;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.clearspring.analytics.util.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import LineDependencies.GPSLine;
import LineDependencies.GeoLine;
import LineDependencies.PossibleShape;
import LineDependencies.ShapeLine;
import LineDependencies.Trip;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;

public class MatchingRoutesShapeGPS {

	private static final double THRESHOLD_TIME = 600000; // 20 minutes
	private static final double PERCENTAGE_DISTANCE = 0.008;
	private static final int THRESHOLD_DISTANCE = 5;

	@SuppressWarnings("serial")
	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.err.println("Please, put the shape file, the GPS file and the output path.");
			System.exit(1);
		}
		
		String pathFileShapes = args[0];
		String pathGPSFile = args[1];
		String pathOutput = args[2];
		
		SparkConf sparkConf = new SparkConf().setAppName("JavaDeduplication").setMaster("local");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);

		Function2<Integer, Iterator<String>, Iterator<String>> removeHeader = 
				new Function2<Integer, Iterator<String>, Iterator<String>>() {
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

		JavaRDD<String> gpsString = ctx.textFile(pathGPSFile, 1).mapPartitionsWithIndex(removeHeader, false);
		JavaRDD<String> shapeString = ctx.textFile(pathFileShapes, 1).mapPartitionsWithIndex(removeHeader, false);

		JavaPairRDD<String, Iterable<GeoPoint>> rddGPSPointsPair = gpsString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(s);
						return new Tuple2<String, GeoPoint>(gpsPoint.getBusCode() + "." + gpsPoint.getLineCode(),
								gpsPoint);
					}
				}).groupByKey();
		
		
		JavaPairRDD<String, Iterable<GeoPoint>> rddShapePointsPair = shapeString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(s);
						return new Tuple2<String, GeoPoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey();
		

		JavaPairRDD<String, GeoLine> rddGPSLinePair = rddGPSPointsPair
				.mapToPair(new PairFunction<Tuple2<String, Iterable<GeoPoint>>, String, GeoLine>() {

					@SuppressWarnings("deprecation")
					GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();

					@Override
					public Tuple2<String, GeoLine> call(Tuple2<String, Iterable<GeoPoint>> pair) throws Exception {

						List<Coordinate> coordinates = new ArrayList<>();
						Double latitude;
						Double longitude;
						String lineBlockingKey = null;

						for (GeoPoint geoPoint : pair._2) {
							latitude = Double.valueOf(geoPoint.getLatitude());
							longitude = Double.valueOf(geoPoint.getLongitude());
							coordinates.add(new Coordinate(latitude, longitude));
							if (lineBlockingKey == null && !((GPSPoint) geoPoint).getLineCode().equals("REC")) {
								lineBlockingKey = ((GPSPoint) geoPoint).getLineCode();
							}
						}

						Coordinate[] array = new Coordinate[coordinates.size()];
						GeoLine geoLine = null;
						try {
							if (array.length > 1 && lineBlockingKey != null) {
								LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
								geoLine = new GPSLine(pair._1, lineString, lineBlockingKey, Lists.newArrayList(pair._2));
							}

						} catch (Exception e) {
							throw new Exception("LineString cannot be created. " + e);
						}

						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});
		

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

						for (GeoPoint geoPoint : pair._2) {
							latitude = Double.valueOf(geoPoint.getLatitude());
							longitude = Double.valueOf(geoPoint.getLongitude());
							coordinates.add(new Coordinate(latitude, longitude));
							lastPoint = (ShapePoint) geoPoint;
							if (lineBlockingKey == null) {
								lineBlockingKey = ((ShapePoint) geoPoint).getRoute();
							}
						}

						Coordinate[] array = new Coordinate[coordinates.size()];

						LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
						String distanceTraveled = lastPoint.getDistanceTraveled();
						String route = lastPoint.getRoute();
						GeoLine geoLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								Lists.newArrayList(pair._2), route);
						
						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});

		JavaPairRDD<String, Iterable<GeoLine>> rddGroupedUnionLines = rddGPSLinePair.union(rddShapeLinePair)
				.groupByKey();
		

		JavaRDD<List<GPSLine>> rddPossibleShapes = rddGroupedUnionLines
				.map(new Function<Tuple2<String, Iterable<GeoLine>>, List<GPSLine>>() {

					@Override
					public List<GPSLine> call(Tuple2<String, Iterable<GeoLine>> entry)
							throws Exception {
						
						List<ShapeLine> shapeLineList = new ArrayList<>();
						List<GPSLine> gpsLineList = new ArrayList<>();

						Iterator<GeoLine> iteratorGeoLine = entry._2.iterator();
						GeoLine geoLine;
						while (iteratorGeoLine.hasNext()) {
							geoLine = iteratorGeoLine.next();

							if (geoLine instanceof ShapeLine) {
								shapeLineList.add((ShapeLine) geoLine);
							} else if (geoLine != null) {
								gpsLineList.add((GPSLine) geoLine);
							}
						}
						
						PossibleShape possibleShape;
						GPSPoint firstPointGPS;
						long timePreviousPointGPS;
						String blockingKeyFromTime = null;
						GPSPoint currentPoint;
						float currentDistanceToStartPoint;
						float currentDistanceToEndPoint;
						int thresholdDistanceCurrentShape;
						
						for (ShapeLine shapeLine : shapeLineList) {
							thresholdDistanceCurrentShape = (int) (shapeLine.getDistanceTraveled() * PERCENTAGE_DISTANCE);
							shapeLine.setThresholdDistance(thresholdDistanceCurrentShape);
							
							for (GPSLine gpsLine : gpsLineList) {
								possibleShape = new PossibleShape(gpsLine.getListGeoPoints(), shapeLine);

								firstPointGPS = (GPSPoint) gpsLine.getListGeoPoints().get(0);
								timePreviousPointGPS = firstPointGPS.getTime();
								
								for (int i = 0; i < gpsLine.getListGeoPoints().size(); i++) {
									currentPoint = (GPSPoint) gpsLine.getListGeoPoints().get(i);

									currentDistanceToStartPoint = possibleShape.getDistanceInMetersToStartPointShape(currentPoint);
									currentDistanceToEndPoint = possibleShape.getDistanceInMetersToEndPointShape(currentPoint);

									if (currentDistanceToStartPoint < thresholdDistanceCurrentShape) {

										if (blockingKeyFromTime == null
												|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME) {
											blockingKeyFromTime = currentPoint.getBlockingKeyFromTime();
										}
										timePreviousPointGPS = currentPoint.getTime();
										possibleShape.addPossibleFirstPoint(new Tuple2<String, Integer>(blockingKeyFromTime, i));

									} else if (currentDistanceToEndPoint < thresholdDistanceCurrentShape) {

										if (blockingKeyFromTime == null
												|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME) {
											blockingKeyFromTime = currentPoint.getBlockingKeyFromTime();
										}
										timePreviousPointGPS = currentPoint.getTime();
										possibleShape.addPossibleLastPoint(new Tuple2<String, Integer>(blockingKeyFromTime, i));
									}
								}
								gpsLine.addPossibleShapeLine(possibleShape);
							}
						}

						return gpsLineList;
					}
				});

		
		JavaRDD<List<GPSLine>> rddFirtAndLastPoints = rddPossibleShapes.map(new Function<List<GPSLine>, List<GPSLine>>() {

					@Override
					public List<GPSLine> call(List<GPSLine> entry) throws Exception {

						List<Tuple2<String, Integer>> possibleFirstGPSPoints;
						List<Tuple2<String, Integer>> possibleLastGPSPoints;
						int biggestListSize;
						
						for (GPSLine gpsLine : entry) {

							for (PossibleShape possibleShape : gpsLine.getListPossibleShapeLines()) {								
								
								possibleFirstGPSPoints = possibleShape.getPossibleFirtsGPSPoints();
								possibleLastGPSPoints = possibleShape.getPossibleLastGPSPoints();
								
								if (!possibleFirstGPSPoints.isEmpty() && possibleLastGPSPoints.isEmpty()  ) {

									possibleShape.addFirstAndLastPoint(possibleFirstGPSPoints.get(0)._2);
									for (int i = 1; i < possibleFirstGPSPoints.size() - 1; i++) {
										possibleShape.addFirstAndLastPoint(possibleFirstGPSPoints.get(i)._2);
										possibleShape.addFirstAndLastPoint(possibleFirstGPSPoints.get(i)._2);
									}

									possibleShape.addFirstAndLastPoint(
											possibleFirstGPSPoints.get(possibleFirstGPSPoints.size() - 1)._2);
									

								} else if (possibleFirstGPSPoints.isEmpty() && !possibleLastGPSPoints.isEmpty()) { 
									
									possibleShape.addFirstAndLastPoint(possibleLastGPSPoints.get(0)._2);
									for (int i = 1; i < possibleLastGPSPoints.size() - 1; i++) {
										possibleShape.addFirstAndLastPoint(possibleLastGPSPoints.get(i)._2);
										possibleShape.addFirstAndLastPoint(possibleLastGPSPoints.get(i)._2);
									}

									possibleShape.addFirstAndLastPoint(
											possibleLastGPSPoints.get(possibleLastGPSPoints.size() - 1)._2);
									
									
								} else {
									biggestListSize = possibleFirstGPSPoints.size() > possibleLastGPSPoints.size()
											? possibleFirstGPSPoints.size() : possibleLastGPSPoints.size();
	
									int indexLastPoint = 0;
									int previousLastPointPosition = 0;
									for (int indexFirstPoint = 0; indexFirstPoint < biggestListSize; indexFirstPoint++) {
	
										if (indexFirstPoint < possibleFirstGPSPoints.size()) {
											int firstPointPosition = possibleFirstGPSPoints.get(indexFirstPoint)._2;
	
											if (indexFirstPoint == 0 || firstPointPosition > previousLastPointPosition) {
	
												for (int iteration = indexLastPoint; iteration < possibleLastGPSPoints
														.size(); iteration++) {
													int lastPointPosition = possibleLastGPSPoints.get(iteration)._2;
	
													if (firstPointPosition < lastPointPosition) {
														possibleShape.addFirstAndLastPoint(firstPointPosition);
														possibleShape.addFirstAndLastPoint(lastPointPosition);
														previousLastPointPosition = lastPointPosition;
														indexLastPoint = iteration;
														break;
													}
												}
											}
										}
									}
								}
							}
						}

						return entry;
					}
				});


		JavaRDD<List<GPSLine>> rddSetUpTrips = rddFirtAndLastPoints.map(new Function<List<GPSLine>, List<GPSLine>>() {

					@Override
					public List<GPSLine> call(List<GPSLine> entry) throws Exception {

						for (GPSLine gpsLine : entry) {
							if (gpsLine != null) {

								if (gpsLine.getListPossibleShapeLines().size() >= 2) {
									
									PossibleShape biggestCoverageShape = null;
									for (PossibleShape possibleShape : gpsLine.getListPossibleShapeLines()) {
										if (biggestCoverageShape == null || biggestCoverageShape
												.getLengthCoverage() < possibleShape.getLengthCoverage()) {
											biggestCoverageShape = possibleShape;
										}										
									}
									Point firstPointFirstShape = gpsLine.getListPossibleShapeLines().get(0).getShapeLine().getLine().getStartPoint();
									Point endPointFirstShape = gpsLine.getListPossibleShapeLines().get(0).getShapeLine().getLine().getEndPoint();
									Point firstPointSecondShape = gpsLine.getListPossibleShapeLines().get(1).getShapeLine().getLine().getStartPoint();
									Point endPointSecondShape = gpsLine.getListPossibleShapeLines().get(1).getShapeLine().getLine().getEndPoint();
								
									if (GPSPoint.getDistanceInMeters(endPointFirstShape.getX(), endPointFirstShape.getY(), firstPointSecondShape.getX(), firstPointSecondShape.getY()) >= THRESHOLD_DISTANCE || 
										GPSPoint.getDistanceInMeters(firstPointFirstShape.getX(), firstPointFirstShape.getY(), endPointSecondShape.getX(), endPointSecondShape.getY()) >= THRESHOLD_DISTANCE) {
										
										List<PossibleShape> possibleShapeCurrentGPS = new ArrayList<>();
										possibleShapeCurrentGPS.add(biggestCoverageShape);
										gpsLine.setListPossibleShapeLines(possibleShapeCurrentGPS);
									}
								}
							}
							gpsLine.setUpTrips();
						}
						return entry;
					}
				});


		JavaRDD<List<GPSLine>> rddClosestPoint = rddSetUpTrips.map(new Function< List<GPSLine>, List<GPSLine>>() {

					@Override
					public List<GPSLine> call(List<GPSLine> entry) throws Exception {

						for (GPSLine gpsLine : entry) {
							for (int numberTrip = 1; numberTrip <= gpsLine.getMapTrips().size(); numberTrip++) {
								for (Trip trip : gpsLine.getMapTrips().get(numberTrip)) {
									for (GeoPoint gpsPoint : trip.getGpsPoints()) {

										double coordXGPS = Double.valueOf(gpsPoint.getLatitude());
										double coordYGPS = Double.valueOf(gpsPoint.getLongitude());
										double coordXShape = Double.valueOf(trip.getShapePoints().get(0).getLatitude());
										double coordYShape = Double.valueOf(trip.getShapePoints().get(0).getLongitude());
										float distanceClosestPoint = GeoPoint.getDistanceInMeters(coordXGPS, coordYGPS,
												coordXShape, coordYShape);
										GeoPoint closestPoint = trip.getShapePoints().get(0);
										for (GeoPoint shapePoint : trip.getShapePoints()) {
											coordXShape = Double.valueOf(shapePoint.getLatitude());
											coordYShape = Double.valueOf(shapePoint.getLongitude());
											float currentDistance = GeoPoint.getDistanceInMeters(coordXGPS, coordYGPS,
													coordXShape, coordYShape);

											if (currentDistance <= distanceClosestPoint) {
												distanceClosestPoint = currentDistance;
												closestPoint = shapePoint;
											}
										}
										((GPSPoint) gpsPoint).setClosestPoint(closestPoint);
										((GPSPoint) gpsPoint).setNumberTrip(numberTrip);
										((GPSPoint) gpsPoint).setDistanceClosestShapePoint(distanceClosestPoint);
										((GPSPoint) gpsPoint).setThresholdShape(trip.getShapeLine().getThresholdDistance());
									}
								}
							}
						}
						
						return entry;
					}
				});

		generateOutputFile(rddClosestPoint, pathOutput);
	
		ctx.stop();
		ctx.close();
	}
	
	private static void generateOutputFile(JavaRDD<List<GPSLine>> rddClosestPoint, String pathOutput) {
		FileWriter output;
		try {
			output = new FileWriter(pathOutput);

			PrintWriter printWriter = new PrintWriter(output);
			printWriter.println("TRIP_NUM,ROUTE,SHAPE_ID,SHAPE_SEQ,LAT_SHAPE,LON_SHAPE,GSP_POINT_ID,BUS_CODE,"
					+ "TIMESTAMP,LAT_GPS,LON_GPS,DISTANCE,THRESHOLD_PROBLEM,PROBLEM");

			for (List<GPSLine> listGPS : rddClosestPoint.collect()) {
				for (GPSLine gpsLine : listGPS) {
					if (gpsLine != null) {
	
						for (int i = 0; i < gpsLine.getMapTrips().size(); i++) {
							for (Trip trip : gpsLine.getTrip(i+1)) {								
								for (GeoPoint geoPoint : trip.getGpsPoints()) {
									
									GPSPoint gpsPoint = (GPSPoint)geoPoint;
									printWriter.print(i+1+",");
									printWriter.print(gpsPoint.getLineCode()+",");
									printWriter.print(gpsPoint.getClosestPoint().getId()+",");
									printWriter.print(gpsPoint.getClosestPoint().getPointSequence()+",");
									printWriter.print(gpsPoint.getClosestPoint().getLatitude()+",");
									printWriter.print(gpsPoint.getClosestPoint().getLongitude()+",");
									printWriter.print(gpsPoint.getGpsId()+",");
									printWriter.print(gpsPoint.getBusCode()+",");
									printWriter.print(gpsPoint.getTimeStamp()+",");
									printWriter.print(gpsPoint.getLatitude()+",");
									printWriter.print(gpsPoint.getLongitude()+",");
									printWriter.print(gpsPoint.getDistanceClosestShapePoint()+",");
									printWriter.print(gpsPoint.getThresholdShape()+",");
									if (gpsPoint.getDistanceClosestShapePoint() > gpsPoint.getThresholdShape()) {
										printWriter.println(gpsPoint.getDistanceClosestShapePoint() 
												- gpsPoint.getThresholdShape());
									} else {
										printWriter.println("-1");
									}
								}									
							}
						}
					}
				}
			}
			
			output.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}