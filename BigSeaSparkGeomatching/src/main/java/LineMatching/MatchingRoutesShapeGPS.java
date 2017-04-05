package LineMatching;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

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
import LineDependencies.Problem;
import LineDependencies.ShapeLine;
import LineDependencies.Trip;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;

public class MatchingRoutesShapeGPS {

	private static final double THRESHOLD_TIME = 600000; // 20 minutes
	private static final double PERCENTAGE_DISTANCE = 0.09;
	private static final String FILE_SEPARATOR = ",";

	public static void main(String[] args) {

		if (args.length < 4) {
			System.err.println("Please, put the shape file, the directory of GPS files, the output path and the number of partitions.");
			System.exit(1);
		}

		String pathFileShapes = args[0];
		String pathGPSFile = args[1];
		String pathOutput = args[2];
		int minPartitions = Integer.valueOf(args[3]);

		SparkConf sparkConf = new SparkConf().setAppName("JavaDeduplication").setMaster("local");
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFiles(pathFileShapes, pathGPSFile, pathOutput, minPartitions, context);
		
		context.stop();
		context.close();
	}
	
	private static void generateOutputFiles(String pathFileShapes, String pathGPSFiles, String pathOutput, int minPartitions, JavaSparkContext context){
		
		File dir = new File(pathGPSFiles);

		for (File file : dir.listFiles()) {

			JavaRDD<List<GPSLine>> rddOutputBuLMA = executeBULMA(pathFileShapes, pathGPSFiles + file.getName(),
					minPartitions, context);
			
			saveOutputFile(rddOutputBuLMA, pathOutput + file.getName());
		}
	}

	@SuppressWarnings("serial")
	private static JavaRDD<List<GPSLine>> executeBULMA(String pathFileShapes, String pathGPSFile, int minPartitions, JavaSparkContext ctx) {
		
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
		
		JavaRDD<String> gpsString = ctx.textFile(pathGPSFile, minPartitions).mapPartitionsWithIndex(removeHeader,
				false);
		JavaRDD<String> shapeString = ctx.textFile(pathFileShapes, minPartitions).mapPartitionsWithIndex(removeHeader,
				false);
	
		JavaPairRDD<String, Iterable<GeoPoint>> rddGPSPointsPair = gpsString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(s);
						return new Tuple2<String, GeoPoint>(gpsPoint.getBusCode(), gpsPoint);

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

							if (lineBlockingKey == null && !((GPSPoint) currentGeoPoint).getLineCode().equals("REC")) {
								lineBlockingKey = ((GPSPoint) currentGeoPoint).getLineCode();
							}
						}

						Coordinate[] array = new Coordinate[coordinates.size()];
						GeoLine geoLine = null;
						try {
							
							if (array.length > 1 && lineBlockingKey != null) {
								LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
								geoLine = new GPSLine(pair._1, lineString, lineBlockingKey, listGeoPoint,
										greaterDistance);
							} else if (array.length >= 1) {
								 geoLine = new GPSLine(pair._1, null, "REC", listGeoPoint, greaterDistance);
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

						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});

		JavaPairRDD<String, Iterable<GeoLine>> rddGroupedUnionLines = rddGPSLinePair.union(rddShapeLinePair)
				.groupByKey();

		JavaRDD<List<GPSLine>> rddPossibleShapes = rddGroupedUnionLines
				.map(new Function<Tuple2<String, Iterable<GeoLine>>, List<GPSLine>>() {

					@Override
					public List<GPSLine> call(Tuple2<String, Iterable<GeoLine>> entry) throws Exception {

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
						int thresholdDistanceCurrentShape = 0;

						for (ShapeLine shapeLine : shapeLineList) {
							thresholdDistanceCurrentShape = (int) (shapeLine.getDistanceTraveled()
									/ (shapeLine.getListGeoPoints().size() * PERCENTAGE_DISTANCE));
							shapeLine.setThresholdDistance(thresholdDistanceCurrentShape);

							for (GPSLine gpsLine : gpsLineList) {
								blockingKeyFromTime = null;
								possibleShape = new PossibleShape(gpsLine.getListGeoPoints(), shapeLine);
								firstPointGPS = (GPSPoint) gpsLine.getListGeoPoints().get(0);
								
								for (int i = 0; i < gpsLine.getListGeoPoints().size(); i++) {
									GPSPoint auxPoint = (GPSPoint) gpsLine.getListGeoPoints().get(i);
									if (!auxPoint.getLineCode().equals("REC")) {
										firstPointGPS = auxPoint;
										break;
									}
								}
																
								timePreviousPointGPS = firstPointGPS.getTime();
								int lastIndexFirst = -2;
								int lastIndexEnd = -2;
								for (int i = 0; i < gpsLine.getListGeoPoints().size(); i++) {
									currentPoint = (GPSPoint) gpsLine.getListGeoPoints().get(i);

									if (!currentPoint.getLineCode().equals("REC")) {
										currentDistanceToStartPoint = possibleShape
												.getDistanceInMetersToStartPointShape(currentPoint);
										currentDistanceToEndPoint = possibleShape
												.getDistanceInMetersToEndPointShape(currentPoint);

										if (currentDistanceToStartPoint < thresholdDistanceCurrentShape) {

											if (blockingKeyFromTime == null
													|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME) {
												if (i > lastIndexFirst + 1) {
													blockingKeyFromTime = currentPoint.getBlockingKeyFromTime();
												}												
												lastIndexFirst = i;
											}
											timePreviousPointGPS = currentPoint.getTime();
											possibleShape.addPossibleFirstPoint(
													new Tuple2<String, Integer>(blockingKeyFromTime, i));
											

										} else if (currentDistanceToEndPoint < thresholdDistanceCurrentShape) {

											if (blockingKeyFromTime == null
													|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME ) {
												if (i > lastIndexEnd + 1) {
													blockingKeyFromTime = currentPoint.getBlockingKeyFromTime();
												}												
												lastIndexEnd = i;
											}

											timePreviousPointGPS = currentPoint.getTime();
											possibleShape.addPossibleLastPoint(
													new Tuple2<String, Integer>(blockingKeyFromTime, i));
											
										}
									}
								}
								gpsLine.addPossibleShapeLine(possibleShape);
							}
						}

						return gpsLineList;
					}
				});
				
		JavaRDD<List<GPSLine>> rddFirtAndLastPoints = rddPossibleShapes
				.map(new Function<List<GPSLine>, List<GPSLine>>() {

					@Override
					public List<GPSLine> call(List<GPSLine> entry) throws Exception {

						Queue<Tuple2<String, Integer>> firstGPSPoints;
						Queue<Tuple2<String, Integer>> lastGPSPoints;

						for (GPSLine gpsLine : entry) {

							if (gpsLine.getListPossibleShapeLines() == null) {
								return entry;
							}
							for (PossibleShape possibleShape : gpsLine.getListPossibleShapeLines()) {

								firstGPSPoints = possibleShape.getFirstGPSPoints();
								lastGPSPoints = possibleShape.getLastGPSPoints();

								if (firstGPSPoints.size() >= 2 && lastGPSPoints.isEmpty()) {

									possibleShape.addFirstAndLastPoint(firstGPSPoints.poll()._2);

									Integer indexPoint;
									while (firstGPSPoints.size() >= 2) {
										indexPoint = firstGPSPoints.poll()._2;
										possibleShape.addFirstAndLastPoint(indexPoint);
										possibleShape.addFirstAndLastPoint(indexPoint + 1);
									}
									possibleShape.addFirstAndLastPoint(firstGPSPoints.poll()._2);

								} else if (firstGPSPoints.isEmpty() && lastGPSPoints.size() >= 2) {

									possibleShape.addFirstAndLastPoint(lastGPSPoints.poll()._2);

									Integer indexPoint;
									while (lastGPSPoints.size() >= 2) {
										indexPoint = lastGPSPoints.poll()._2;
										possibleShape.addFirstAndLastPoint(indexPoint);
										possibleShape.addFirstAndLastPoint(indexPoint + 1);								
									}
									possibleShape.addFirstAndLastPoint(lastGPSPoints.poll()._2);

								} else {

									int previousLastPointPosition = 0;
									boolean isFirstTrip = true;
									while (!firstGPSPoints.isEmpty() && !lastGPSPoints.isEmpty()) {

										int firstPointPosition = firstGPSPoints.poll()._2;
										if (isFirstTrip || firstPointPosition > previousLastPointPosition) {

											while (!lastGPSPoints.isEmpty()) {
												int lastPointPosition = lastGPSPoints.poll()._2;

												if (firstPointPosition < lastPointPosition) {

													possibleShape.addFirstAndLastPoint(firstPointPosition);
													possibleShape.addFirstAndLastPoint(lastPointPosition);
													previousLastPointPosition = lastPointPosition;

													break;

												} else if (!isFirstTrip) {
													possibleShape.addFirstAndLastPoint(previousLastPointPosition * -1);
													possibleShape.addFirstAndLastPoint(lastPointPosition * -1);
												}
											}
										} else if (!isFirstTrip) {

											Integer notProblem = possibleShape.getListIndexFirstAndLastGPSPoints()
													.remove(possibleShape.getListIndexFirstAndLastGPSPoints().size()
															- 1);
											Integer problem = possibleShape.getListIndexFirstAndLastGPSPoints().remove(
													possibleShape.getListIndexFirstAndLastGPSPoints().size() - 1);

											possibleShape.addFirstAndLastPoint(problem * -1);
											possibleShape.addFirstAndLastPoint(firstPointPosition * -1);
											possibleShape.addFirstAndLastPoint(firstPointPosition + 1); 
											possibleShape.addFirstAndLastPoint(notProblem);
										}

										isFirstTrip = false;
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

					if (gpsLine != null && gpsLine.getListPossibleShapeLines() != null) {
						List<PossibleShape> listPossibleShapeLines = gpsLine.getListPossibleShapeLines();

						if (listPossibleShapeLines.size() >= 2) {

							PossibleShape bestShape = null;
							Long timeFirstPoint = null;
							float sumGreaterDistancePoints = 0;
							float thresholdShapesSequence;
							for (PossibleShape possibleShape : listPossibleShapeLines) {

								sumGreaterDistancePoints += possibleShape.getShapeLine().getGreaterDistancePoints();
								if (possibleShape.getListIndexFirstAndLastGPSPoints().size() >= 1) {
									GPSPoint firstPointCurrentPossibleShape = ((GPSPoint) possibleShape
											.getListGPSPoints()
											.get(Math.abs(possibleShape.getListIndexFirstAndLastGPSPoints().get(0))));
									if (timeFirstPoint == null
											|| firstPointCurrentPossibleShape.getTime() < timeFirstPoint) {
										timeFirstPoint = firstPointCurrentPossibleShape.getTime();
										bestShape = possibleShape;
									}
								}
							}

							thresholdShapesSequence = sumGreaterDistancePoints / listPossibleShapeLines.size();

							Point firstPointFirstShape = listPossibleShapeLines.get(0).getShapeLine().getLine()
									.getStartPoint();
							Point endPointFirstShape = listPossibleShapeLines.get(0).getShapeLine().getLine()
									.getEndPoint();
							Point firstPointSecondShape = listPossibleShapeLines.get(1).getShapeLine().getLine()
									.getStartPoint();
							Point endPointSecondShape = listPossibleShapeLines.get(1).getShapeLine().getLine()
									.getEndPoint();

							if (GPSPoint.getDistanceInMeters(endPointFirstShape,
									firstPointSecondShape) >= thresholdShapesSequence
									&& GPSPoint.getDistanceInMeters(firstPointFirstShape,
											endPointSecondShape) >= thresholdShapesSequence) {

								List<PossibleShape> possibleShapeCurrentGPS = new ArrayList<>();
								possibleShapeCurrentGPS.add(bestShape);
								gpsLine.setListPossibleShapeLines(possibleShapeCurrentGPS);

							}
						}

						gpsLine.setUpTrips();
					}
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
							if (trip.getShapeLine() != null) {
								for (GeoPoint gpsPoint : trip.getGpsPoints()) {
									
									GeoPoint closestPoint = trip.getShapePoints().get(0);
									float distanceClosestPoint = GeoPoint.getDistanceInMeters(gpsPoint, closestPoint);
									
									for (GeoPoint currentShapePoint : trip.getShapePoints()) {
										float currentDistance = GeoPoint.getDistanceInMeters(gpsPoint, currentShapePoint);

										if (currentDistance <= distanceClosestPoint) {
											distanceClosestPoint = currentDistance;
											closestPoint = currentShapePoint;
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
				}				
				return entry;
			}
		});

		return rddClosestPoint;		
	}

	private static void saveOutputFile(JavaRDD<List<GPSLine>> rddClosestPoint, String pathOutput) {
		FileWriter output;
		try {
			output = new FileWriter(pathOutput);

			PrintWriter printWriter = new PrintWriter(output);
			printWriter.print("TRIP_NUM" + FILE_SEPARATOR);
			printWriter.print("ROUTE" + FILE_SEPARATOR);
			printWriter.print("SHAPE_ID" + FILE_SEPARATOR);
			printWriter.print("SHAPE_SEQ" + FILE_SEPARATOR);
			printWriter.print("LAT_SHAPE" + FILE_SEPARATOR);
			printWriter.print("LON_SHAPE" + FILE_SEPARATOR);
			printWriter.print("GPS_POINT_ID" + FILE_SEPARATOR);
			printWriter.print("BUS_CODE" + FILE_SEPARATOR);
			printWriter.print("TIMESTAMP" + FILE_SEPARATOR);
			printWriter.print("LAT_GPS" + FILE_SEPARATOR);
			printWriter.print("LON_GPS" + FILE_SEPARATOR);
			printWriter.print("DISTANCE" + FILE_SEPARATOR);
			printWriter.print("THRESHOLD_PROBLEM" + FILE_SEPARATOR);
			printWriter.println("TRIP_PROBLEM");

			for (List<GPSLine> listGPS : rddClosestPoint.collect()) {
				for (GPSLine gpsLine : listGPS) {

					if (gpsLine != null) {

						if (gpsLine.getMapTrips().isEmpty()) {
							GPSPoint gpsPoint;
							for (GeoPoint geoPoint: gpsLine.getListGeoPoints()) {
								gpsPoint = (GPSPoint) geoPoint;
								printWriter.print(Problem.NO_TRIP.getCode() + FILE_SEPARATOR);
								printWriter.print(gpsPoint.getLineCode() + FILE_SEPARATOR);
								
								printWriter.print("-" + FILE_SEPARATOR);
								printWriter.print("-"  + FILE_SEPARATOR);
								printWriter.print("-"  + FILE_SEPARATOR);
								printWriter.print("-" + FILE_SEPARATOR);
								

								printWriter.print(gpsPoint.getGpsId() + FILE_SEPARATOR);
								printWriter.print(gpsPoint.getBusCode() + FILE_SEPARATOR);
								printWriter.print(gpsPoint.getTimeStamp() + FILE_SEPARATOR);
								printWriter.print(gpsPoint.getLatitude() + FILE_SEPARATOR);
								printWriter.print(gpsPoint.getLongitude() + FILE_SEPARATOR);
								
								printWriter.print("-" + FILE_SEPARATOR);
								printWriter.print("-" + FILE_SEPARATOR);
								
								printWriter.println(Problem.NO_TRIP.getCode());
								
							}
						}

						for (Integer key : gpsLine.getMapTrips().keySet()) {
							for (Trip trip : gpsLine.getTrip(key)) {

								for (GeoPoint geoPoint : trip.getGPSPoints()) {

									GPSPoint gpsPoint = (GPSPoint) geoPoint;

									printWriter.print(key  + FILE_SEPARATOR);
									printWriter.print(gpsPoint.getLineCode()  + FILE_SEPARATOR);
									if (trip.getShapeLine() == null) {
										printWriter.print("-" + FILE_SEPARATOR);
										printWriter.print("-" + FILE_SEPARATOR);
										printWriter.print("-" + FILE_SEPARATOR);
										printWriter.print("-" + FILE_SEPARATOR);
									} else {
										printWriter.print(gpsPoint.getClosestPoint().getId() + FILE_SEPARATOR);
										printWriter.print(gpsPoint.getClosestPoint().getPointSequence() + FILE_SEPARATOR);
										printWriter.print(gpsPoint.getClosestPoint().getLatitude() + FILE_SEPARATOR);
										printWriter.print(gpsPoint.getClosestPoint().getLongitude() + FILE_SEPARATOR);
									}

									printWriter.print(gpsPoint.getGpsId() + FILE_SEPARATOR);
									printWriter.print(gpsPoint.getBusCode() + FILE_SEPARATOR);
									printWriter.print(gpsPoint.getTimeStamp() + FILE_SEPARATOR);
									printWriter.print(gpsPoint.getLatitude() + FILE_SEPARATOR);
									printWriter.print(gpsPoint.getLongitude() + FILE_SEPARATOR);

									if (trip.getShapeLine() == null) {
										printWriter.print("-" + FILE_SEPARATOR);
										printWriter.print("-" + FILE_SEPARATOR);
									} else {
										printWriter.print(gpsPoint.getDistanceClosestShapePoint() + FILE_SEPARATOR);
										printWriter.print(gpsPoint.getThresholdShape() + FILE_SEPARATOR);
									}

									if (trip.getProblem().equals(Problem.TRIP_PROBLEM)) {
										printWriter.println(trip.getProblem().getCode());
									} else if (gpsPoint.getDistanceClosestShapePoint() > gpsPoint.getThresholdShape()) {
										printWriter.println(Problem.POINT_ABOVE_THRESHOLD.getCode());
									} else {
										printWriter.println(trip.getProblem().getCode());
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