package BULMA;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Sleeper;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.scalatest.concurrent.AsyncAssertions.Waiter;
import org.scalatest.words.FullyMatchWord;

import com.clearspring.analytics.util.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import BULMADependences.GPSLine;
import BULMADependences.GeoLine;
import BULMADependences.PossibleShape;
import BULMADependences.Problem;
import BULMADependences.ShapeLine;
import BULMADependences.Trip;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;

public class MatchingRoutesShapeGPS {

	private static final double THRESHOLD_TIME = 600000; // 20 minutes
	private static final double PERCENTAGE_DISTANCE = 0.09;
	private static final String FILE_SEPARATOR = ",";
	private static final String SLASH = "/";

	public static void main(String[] args) throws IOException, URISyntaxException {

		if (args.length < 4) {
			System.err.println(
					"Usage: <shape file> <directory of GPS files> <directory of output path> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();

		String pathFileShapes = args[0];
		String pathGPSFile = args[1];
		String pathOutput = args[2];
		int minPartitions = Integer.valueOf(args[3]);

		SparkConf sparkConf = new SparkConf().setAppName("BULMA").setMaster("local");
		// SparkConf sparkConf = new SparkConf().setAppName("BULMA");
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		generateOutputFiles(pathFileShapes, pathGPSFile, pathOutput, minPartitions, context);

		context.stop();
		context.close();
		System.out.println("Execution time: " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - initialTime) + " min");
	}

	private static void generateOutputFiles(String pathFileShapes, String pathGPSFiles, String pathOutput,
			int minPartitions, JavaSparkContext context) throws IOException, URISyntaxException {

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(new URI(pathGPSFiles), conf);
		FileStatus[] fileStatus = fs.listStatus(new Path(pathGPSFiles));

		for (FileStatus file : fileStatus) {
			JavaRDD<String> rddOutputBuLMA = executeBULMA(pathFileShapes, pathGPSFiles + file.getPath().getName(),
					minPartitions, context);

			rddOutputBuLMA.saveAsTextFile(pathOutput + SLASH
					+ file.getPath().getName().substring(0, file.getPath().getName().lastIndexOf(".csv")));
		}
	}

	@SuppressWarnings("serial")
	private static JavaRDD<String> executeBULMA(String pathFileShapes, String pathGPSFile, int minPartitions,
			JavaSparkContext ctx) {

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

		JavaRDD<String> gpsString = ctx.textFile(pathGPSFile, minPartitions).mapPartitionsWithIndex(removeHeader,
				false);
		JavaRDD<String> shapeString = ctx.textFile(pathFileShapes, minPartitions).mapPartitionsWithIndex(removeHeader,
				false);

		JavaPairRDD<String, Iterable<GeoPoint>> rddGPSPointsPair = gpsString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(s);

						return new Tuple2<String, GeoPoint>(gpsPoint.getBusCode(), gpsPoint);
					}

				}).groupByKey(minPartitions);

		JavaPairRDD<String, Iterable<GeoPoint>> rddShapePointsPair = shapeString
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(s);
						return new Tuple2<String, GeoPoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey(minPartitions);

		JavaPairRDD<String, GeoLine> rddGPSLinePair = rddGPSPointsPair
				.mapToPair(new PairFunction<Tuple2<String, Iterable<GeoPoint>>, String, GeoLine>() {

					@SuppressWarnings("deprecation")
					GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();

					public Tuple2<String, GeoLine> call(Tuple2<String, Iterable<GeoPoint>> pair) throws Exception {

						List<Coordinate> coordinates = new ArrayList<Coordinate>();
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

					public Tuple2<String, GeoLine> call(Tuple2<String, Iterable<GeoPoint>> pair) throws Exception {

						List<Coordinate> coordinates = new ArrayList<Coordinate>();
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
						Float distanceTraveled = lastPoint.getDistanceTraveled();
						String route = lastPoint.getRoute();
						GeoLine geoLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								listGeoPoint, route, greaterDistance);

						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});

		JavaPairRDD<String, Iterable<GeoLine>> rddGroupedUnionLines = rddGPSLinePair.union(rddShapeLinePair)
				.groupByKey(minPartitions);

		JavaRDD<Tuple2<GPSLine, List<PossibleShape>>> rddPossibleShapes = rddGroupedUnionLines.flatMap(
				new FlatMapFunction<Tuple2<String, Iterable<GeoLine>>, Tuple2<GPSLine, List<PossibleShape>>>() {

					@Override
					public Iterator<Tuple2<GPSLine, List<PossibleShape>>> call(Tuple2<String, Iterable<GeoLine>> entry)
							throws Exception {

						List<ShapeLine> shapeLineList = new ArrayList<ShapeLine>();
						List<GPSLine> gpsLineList = new ArrayList<GPSLine>();

						List<Tuple2<GPSLine, List<PossibleShape>>> listOutput = new LinkedList<Tuple2<GPSLine, List<PossibleShape>>>();

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

						while (!gpsLineList.isEmpty()) {
							GPSLine gpsLine = gpsLineList.remove(0);

							List<PossibleShape> listPossibleShapes = new LinkedList<PossibleShape>();
							for (ShapeLine shapeLine : shapeLineList) {
								thresholdDistanceCurrentShape = (int) (shapeLine.getDistanceTraveled()
										/ (shapeLine.getListGeoPoints().size() * PERCENTAGE_DISTANCE));
								shapeLine.setThresholdDistance(thresholdDistanceCurrentShape);

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
													|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME) {
												if (i > lastIndexEnd + 1) {
													blockingKeyFromTime = currentPoint.getBlockingKeyFromTime();
												}
												lastIndexEnd = i;
											}

											timePreviousPointGPS = currentPoint.getTime();

											if (possibleShape.isRoundShape()) {
												possibleShape.addPossibleFirstPoint(
														new Tuple2<String, Integer>(blockingKeyFromTime, i));
											} else {
												possibleShape.addPossibleLastPoint(
														new Tuple2<String, Integer>(blockingKeyFromTime, i));
											}

										}
									}
								}
								listPossibleShapes.add(possibleShape);

						
							}
							listOutput.add(new Tuple2<GPSLine, List<PossibleShape>>(gpsLine, listPossibleShapes));
						}

						return listOutput.iterator();
					}
				});

		JavaRDD<Tuple2<GPSLine, List<PossibleShape>>> rddTrueShapes = rddPossibleShapes
				.map(new Function<Tuple2<GPSLine, List<PossibleShape>>, Tuple2<GPSLine, List<PossibleShape>>>() {

					@Override
					public Tuple2<GPSLine, List<PossibleShape>> call(Tuple2<GPSLine, List<PossibleShape>> entry)
							throws Exception {

						Queue<Tuple2<String, Integer>> firstGPSPoints;
						Queue<Tuple2<String, Integer>> lastGPSPoints;

						Map<GPSLine, List<PossibleShape>> mapOutput = new HashMap<GPSLine, List<PossibleShape>>();

						boolean hasRoundShape = false;

						List<PossibleShape> possibleShapesList = entry._2;
						GPSLine gpsLine = entry._1;

						for (PossibleShape possibleShape : possibleShapesList) {
							if (possibleShape.isRoundShape()) {
								hasRoundShape = true;
							}

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
												.remove(possibleShape.getListIndexFirstAndLastGPSPoints().size() - 1);
										Integer problem = possibleShape.getListIndexFirstAndLastGPSPoints()
												.remove(possibleShape.getListIndexFirstAndLastGPSPoints().size() - 1);

										possibleShape.addFirstAndLastPoint(problem * -1);
										possibleShape.addFirstAndLastPoint(firstPointPosition * -1);
										possibleShape.addFirstAndLastPoint(firstPointPosition + 1);
										possibleShape.addFirstAndLastPoint(notProblem);
									}

									isFirstTrip = false;
								}
							}

						}
						Collections.sort(possibleShapesList);
						if (!hasRoundShape && possibleShapesList.size() > 2) {
							
							Integer indexSmaller = null;
							Integer indexSmaller2 = null;
							Integer numberPoints1 = null;
							Integer numberPoints2 = null;
							PossibleShape possibleShape1 = null;
							PossibleShape possibleShape2 = null;

							for (PossibleShape possibleShape : possibleShapesList) {
								if (possibleShape.getListIndexFirstAndLastGPSPoints().size() > 2) {
									int value = Math.abs(possibleShape.getListIndexFirstAndLastGPSPoints().get(0));
									int value2 = Math.abs(possibleShape.getListIndexFirstAndLastGPSPoints().get(1));

									int difference = value2 - value;

									if (indexSmaller == null || value < indexSmaller) {

										indexSmaller2 = indexSmaller;
										possibleShape2 = possibleShape1;
										numberPoints2 = numberPoints1;
										indexSmaller = value;
										possibleShape1 = possibleShape;
										numberPoints1 = difference;

									} else if (indexSmaller2 == null || value < indexSmaller2) {
										indexSmaller2 = value;
										possibleShape2 = possibleShape;
										numberPoints2 = difference;
									}
								}

							}

							if (numberPoints1 != null && numberPoints2 != null && numberPoints1 > numberPoints2) {
								possibleShapesList = findComplementaryShape(possibleShape1, possibleShapesList);
							} else if (numberPoints1 != null && numberPoints2 != null) {
								possibleShapesList = findComplementaryShape(possibleShape2, possibleShapesList);
							}

						} else if (hasRoundShape && possibleShapesList.size() > 1) {

							PossibleShape bestShape = null;
							Long timeFirstPoint = null;
							Integer numberPoints = null;

							for (PossibleShape possibleShape : possibleShapesList) {

								if (possibleShape.getListIndexFirstAndLastGPSPoints().size() >= 1) {
									GPSPoint firstPointCurrentPossibleShape = ((GPSPoint) possibleShape
											.getListGPSPoints()
											.get(Math.abs(possibleShape.getListIndexFirstAndLastGPSPoints().get(0))));

									if (timeFirstPoint == null
											|| firstPointCurrentPossibleShape.getTime() < timeFirstPoint) {
										timeFirstPoint = firstPointCurrentPossibleShape.getTime();
										bestShape = possibleShape;
										numberPoints = possibleShape.getListIndexFirstAndLastGPSPoints().get(1)
												- possibleShape.getListIndexFirstAndLastGPSPoints().get(0);

									} else if (firstPointCurrentPossibleShape.getTime() == timeFirstPoint
											&& (possibleShape.getListIndexFirstAndLastGPSPoints().get(1) - possibleShape
													.getListIndexFirstAndLastGPSPoints().get(0)) > numberPoints) {
										bestShape = possibleShape;
										numberPoints = possibleShape.getListIndexFirstAndLastGPSPoints().get(1)
												- possibleShape.getListIndexFirstAndLastGPSPoints().get(0);
									}
								}
							}

							List<PossibleShape> possibleShapeCurrentGPS = new ArrayList<PossibleShape>();
							possibleShapeCurrentGPS.add(bestShape);
							possibleShapesList = possibleShapeCurrentGPS;
						}

						mapOutput.put(gpsLine, possibleShapesList);
						return new Tuple2<GPSLine, List<PossibleShape>>(gpsLine, possibleShapesList);
					}

					private List<PossibleShape> findComplementaryShape(PossibleShape entryShape,
							List<PossibleShape> possibleShapes) {
						PossibleShape complementaryShape = null;
						Point firstPointEntryShape = entryShape.getShapeLine().getLine().getStartPoint();
						Point endPointEntryShape = entryShape.getShapeLine().getLine().getEndPoint();

						for (PossibleShape possibleShape : possibleShapes) {
							Point currentStartPoint = possibleShape.getShapeLine().getLine().getStartPoint();
							Point currentEndPoint = possibleShape.getShapeLine().getLine().getEndPoint();

							if (GeoPoint.getDistanceInMeters(firstPointEntryShape, currentEndPoint) < possibleShape
									.getShapeLine().getGreaterDistancePoints()
									&& GeoPoint.getDistanceInMeters(endPointEntryShape,
											currentStartPoint) < possibleShape.getShapeLine()
													.getGreaterDistancePoints()) {
								complementaryShape = possibleShape;
								break;
							}

						}

						List<PossibleShape> newList = new ArrayList<PossibleShape>();
						newList.add(entryShape);
						newList.add(complementaryShape);

						return newList;
					}
				});

		JavaRDD<Tuple2<GPSLine, Map<Integer, List<Trip>>>> rddTrips = rddTrueShapes
				.map(new Function<Tuple2<GPSLine, List<PossibleShape>>, Tuple2<GPSLine, Map<Integer, List<Trip>>>>() {

					@Override
					public Tuple2<GPSLine, Map<Integer, List<Trip>>> call(Tuple2<GPSLine, List<PossibleShape>> entry)
							throws Exception {

						Map<GPSLine, Map<Integer, List<Trip>>> mapOutput = new HashMap<GPSLine, Map<Integer, List<Trip>>>();

						GPSLine gpsLine = entry._1;
						List<PossibleShape> possibleShapesList = entry._2();
						Map<Integer, List<Trip>> mapTrips = new HashMap<Integer, List<Trip>>();

						for (PossibleShape possibleShape : possibleShapesList) {
							if (possibleShape != null) {
								int numberTrip = 1;

								for (int i = 0; i < possibleShape.getListIndexFirstAndLastGPSPoints().size()
										- 1; i += 2) {
									boolean isTripProblem = false;
									int firstIndex = possibleShape.getListIndexFirstAndLastGPSPoints().get(i);
									int lastIndex = possibleShape.getListIndexFirstAndLastGPSPoints().get(i + 1);

									if (lastIndex < 0) {
										isTripProblem = true;
										firstIndex *= -1;
										lastIndex *= -1;
									}

									if (isTripProblem) {
										addTrip(gpsLine.getListGeoPoints(), mapTrips, numberTrip++, firstIndex,
												lastIndex, possibleShape.getShapeLine(), Problem.TRIP_PROBLEM);
									} else {
										addTrip(gpsLine.getListGeoPoints(), mapTrips, numberTrip++, firstIndex,
												lastIndex, possibleShape.getShapeLine(), Problem.NO_PROBLEM);
									}

								}
							}
						}

						setUpOutliers(gpsLine.getListGeoPoints(), mapTrips);

						mapOutput.put(gpsLine, mapTrips);

						return new Tuple2<GPSLine, Map<Integer, List<Trip>>>(gpsLine, mapTrips);
					}

					private void setUpOutliers(List<GeoPoint> listGeoPoints, Map<Integer, List<Trip>> mapTrips) {

						for (int i = 1; i <= mapTrips.keySet().size(); i++) {

							List<Trip> currentlistTrip = mapTrips.get(i);

							if (!currentlistTrip.isEmpty()) {

								ArrayList<GeoPoint> pointsTripGPS;
								int currentLastIndex = currentlistTrip.get(currentlistTrip.size() - 1).getLastIndex();

								if (i == 1 && !currentlistTrip.isEmpty()) {
									int currentFirstIndex = currentlistTrip.get(0).getFirstIndex();
									if (currentFirstIndex > 0) {
										pointsTripGPS = new ArrayList<GeoPoint>();
										pointsTripGPS.addAll(listGeoPoints.subList(0, currentFirstIndex));
										try {
											currentlistTrip.add(0,
													new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT));
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
									
								} else if (i > 1 && i == mapTrips.keySet().size()) {
									if (listGeoPoints.size() - 1 > currentLastIndex) {

										pointsTripGPS = new ArrayList<GeoPoint>();
										pointsTripGPS.addAll(
												listGeoPoints.subList(currentLastIndex + 1, listGeoPoints.size()));
										try {
											currentlistTrip.add(new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT));
										} catch (Exception e) {
											e.printStackTrace();
										}
									}

								} else if (i > 1) {
									List<Trip> nextListTrip = mapTrips.get(i + 1);
									if (!nextListTrip.isEmpty()) {
										int nextFirstIndex = nextListTrip.get(0).getFirstIndex();
										if (nextFirstIndex > currentLastIndex + 1) {

											pointsTripGPS = new ArrayList<GeoPoint>();
											pointsTripGPS.addAll(
													listGeoPoints.subList(currentLastIndex + 1, nextFirstIndex));

											try {
												currentlistTrip
														.add(new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT));
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									}
								}
							}
							mapTrips.put(i, currentlistTrip);
						}
					}

					private void addTrip(List<GeoPoint> listGeoPoints, Map<Integer, List<Trip>> mapTrips,
							Integer numberTrip, Integer firstIndex, Integer lastIndex, ShapeLine shapeLine,
							Problem problem) {

						if (numberTrip > 1) {
							List<Trip> previousTrip = mapTrips.get(numberTrip - 1);
							if (!previousTrip.isEmpty()) {
								int lastIndexPreviousTrip = previousTrip.get(previousTrip.size() - 1).getLastIndex()
										+ 1;
								if (firstIndex < lastIndexPreviousTrip) {
									firstIndex = lastIndexPreviousTrip;
								}
							}
						}

						if (numberTrip < mapTrips.keySet().size()) {
							List<Trip> nextTrip = mapTrips.get(numberTrip + 1);
							if (!nextTrip.isEmpty()) {
								int firstIndexNextTrip = nextTrip.get(0).getFirstIndex();
								if (lastIndex >= firstIndexNextTrip) {
									lastIndex = firstIndexNextTrip;
								}
							}
						}

						if (!mapTrips.containsKey(numberTrip)) {
							mapTrips.put(numberTrip, new ArrayList<Trip>());
						}

						List<Trip> listTrips = mapTrips.get(numberTrip);
						ArrayList<GeoPoint> pointsTripGPS;
						Trip newTrip = null;

						if (!listTrips.isEmpty()) {
							int indexPreviousLastPoint = listTrips.get(listTrips.size() - 1).getLastIndex() + 1;
							if (firstIndex > indexPreviousLastPoint) {

								pointsTripGPS = new ArrayList<GeoPoint>();
								pointsTripGPS.addAll(listGeoPoints.subList(indexPreviousLastPoint, firstIndex));

								try {
									newTrip = new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT);
									newTrip.setFirstIndex(indexPreviousLastPoint);
									newTrip.setLastIndex(firstIndex);
									listTrips.add(newTrip);
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else if (firstIndex < indexPreviousLastPoint) {
								firstIndex = indexPreviousLastPoint;
							}
						}

						if (firstIndex < lastIndex) { 
							pointsTripGPS = new ArrayList<GeoPoint>();
							pointsTripGPS.addAll(listGeoPoints.subList(firstIndex, lastIndex + 1));

							try {
								newTrip = new Trip(shapeLine, pointsTripGPS, problem);
								newTrip.setFirstIndex(firstIndex);
								newTrip.setLastIndex(lastIndex);
								listTrips.add(newTrip);
								
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						mapTrips.put(numberTrip, listTrips);
					}

				});

		JavaRDD<Tuple2<GPSLine, Map<Integer, List<Trip>>>> rddClosestPoint = rddTrips.map(
				new Function<Tuple2<GPSLine, Map<Integer, List<Trip>>>, Tuple2<GPSLine, Map<Integer, List<Trip>>>>() {

					@Override
					public Tuple2<GPSLine, Map<Integer, List<Trip>>> call(
							Tuple2<GPSLine, Map<Integer, List<Trip>>> entry) throws Exception {


						GPSLine gpsLine = entry._1;
						Map<Integer, List<Trip>> mapTrips = entry._2;

						for (int numberTrip = 1; numberTrip <= mapTrips.size(); numberTrip++) {

							for (Trip trip : mapTrips.get(numberTrip)) {
								if (trip.getShapeLine() != null) {
									for (GeoPoint gpsPoint : trip.getGpsPoints()) {

										GeoPoint closestPoint = trip.getShapePoints().get(0);
										float distanceClosestPoint = GeoPoint.getDistanceInMeters(gpsPoint,
												closestPoint);

										for (GeoPoint currentShapePoint : trip.getShapePoints()) {
											float currentDistance = GeoPoint.getDistanceInMeters(gpsPoint,
													currentShapePoint);

											if (currentDistance <= distanceClosestPoint) {
												distanceClosestPoint = currentDistance;
												closestPoint = currentShapePoint;
											}
										}

										((GPSPoint) gpsPoint).setClosestPoint(closestPoint);
										((GPSPoint) gpsPoint).setNumberTrip(numberTrip);
										((GPSPoint) gpsPoint).setDistanceClosestShapePoint(distanceClosestPoint);
										((GPSPoint) gpsPoint)
												.setThresholdShape(trip.getShapeLine().getThresholdDistance());
									}
								}
							}
						}

						return new Tuple2<GPSLine, Map<Integer, List<Trip>>>(gpsLine, mapTrips);
					}
				});

		JavaRDD<String> rddOutput = rddClosestPoint
				.flatMap(new FlatMapFunction<Tuple2<GPSLine, Map<Integer, List<Trip>>>, String>() {

					public Iterator<String> call(Tuple2<GPSLine, Map<Integer, List<Trip>>> entry) throws Exception {

						List<String> listOutput = new ArrayList<String>();

						GPSLine gpsLine = entry._1;
						Map<Integer, List<Trip>> mapTrips = entry._2;

						if (gpsLine != null) {

							if (mapTrips.isEmpty()) {
								GPSPoint gpsPoint;
								for (GeoPoint geoPoint : gpsLine.getListGeoPoints()) {
									String stringOutput = "";
									
									gpsPoint = (GPSPoint) geoPoint;
									stringOutput += Problem.NO_SHAPE.getCode() + FILE_SEPARATOR;
									stringOutput += gpsPoint.getLineCode() + FILE_SEPARATOR;

									stringOutput += "-" + FILE_SEPARATOR;
									stringOutput += "-" + FILE_SEPARATOR;
									stringOutput += "-" + FILE_SEPARATOR;
									stringOutput += "-" + FILE_SEPARATOR;

									stringOutput += gpsPoint.getGpsId() + FILE_SEPARATOR;
									stringOutput += gpsPoint.getBusCode() + FILE_SEPARATOR;
									stringOutput += gpsPoint.getTimeStamp() + FILE_SEPARATOR;
									stringOutput += gpsPoint.getLatitude() + FILE_SEPARATOR;
									stringOutput += gpsPoint.getLongitude() + FILE_SEPARATOR;

									stringOutput += "-" + FILE_SEPARATOR;
									stringOutput += "-" + FILE_SEPARATOR;

									stringOutput += Problem.NO_SHAPE.getCode();
									listOutput.add(stringOutput);

								}
							}

							for (Integer key : mapTrips.keySet()) {
								for (Trip trip : mapTrips.get(key)) {

									for (GeoPoint geoPoint : trip.getGPSPoints()) {

										GPSPoint gpsPoint = (GPSPoint) geoPoint;
										String stringOutput = "";
										stringOutput += key + FILE_SEPARATOR;
										stringOutput += gpsPoint.getLineCode() + FILE_SEPARATOR;
										if (trip.getShapeLine() == null) {
											stringOutput += "-" + FILE_SEPARATOR;
											stringOutput += "-" + FILE_SEPARATOR;
											stringOutput += "-" + FILE_SEPARATOR;
											stringOutput += "-" + FILE_SEPARATOR;
										} else {
											stringOutput += gpsPoint.getClosestPoint().getId() + FILE_SEPARATOR;
											stringOutput += gpsPoint.getClosestPoint().getPointSequence()
													+ FILE_SEPARATOR;
											stringOutput += gpsPoint.getClosestPoint().getLatitude() + FILE_SEPARATOR;
											stringOutput += gpsPoint.getClosestPoint().getLongitude() + FILE_SEPARATOR;
										}

										stringOutput += gpsPoint.getGpsId() + FILE_SEPARATOR;
										stringOutput += gpsPoint.getBusCode() + FILE_SEPARATOR;
										stringOutput += gpsPoint.getTimeStamp() + FILE_SEPARATOR;
										stringOutput += gpsPoint.getLatitude() + FILE_SEPARATOR;
										stringOutput += gpsPoint.getLongitude() + FILE_SEPARATOR;

										if (trip.getShapeLine() == null) {
											stringOutput += "-" + FILE_SEPARATOR;
											stringOutput += "-" + FILE_SEPARATOR;
										} else {
											stringOutput += gpsPoint.getDistanceClosestShapePoint() + FILE_SEPARATOR;
											stringOutput += gpsPoint.getThresholdShape() + FILE_SEPARATOR;
										}

										if (trip.getProblem().equals(Problem.TRIP_PROBLEM)) {
											stringOutput += trip.getProblem().getCode();
										} else if (gpsPoint.getDistanceClosestShapePoint() > gpsPoint
												.getThresholdShape()) {
											stringOutput += Problem.OUTLIER_POINT.getCode();
										} else {
											stringOutput += trip.getProblem().getCode();
										}
										listOutput.add(stringOutput);
									}
								}
							}
						}

						return listOutput.iterator();
					}
				});

		return rddOutput;
	}

}