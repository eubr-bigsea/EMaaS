package BULMAversion20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.clearspring.analytics.util.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import BULMADependences.BULMAFieldsInputGPS;
import BULMADependences.BULMAFieldsInputShape;
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

public class MatchingRoutesVersion2 {

	private static final double THRESHOLD_TIME = 600000; // 20 minutes
	private static final double PERCENTAGE_DISTANCE = 0.09;
	private static final String FILE_SEPARATOR = ",";
	private static final int NUMBER_ATTRIBUTES_INPUTS = 6;
	private static int[] arrayIndexFieldsInputGPS = new int[NUMBER_ATTRIBUTES_INPUTS];
	private static int[] arrayIndexFieldsInputShape = new int[NUMBER_ATTRIBUTES_INPUTS];

	/**
	 * Generates DataFrames from csv input files
	 */
	public static Dataset<Tuple2<String, GeoLine>> generateDataFrames(String pathShapeFile, String pathGPSFile,
			Integer minPartitions, SparkSession spark, String paramsGPS, String paramsShapes) throws Exception {

		Dataset<Row> datasetGPSFile = spark.read().text(pathGPSFile);
		Dataset<Row> datasetShapesFile = spark.read().text(pathShapeFile);
		BULMAFieldsInputGPS paramsDatasetGPS = new BULMAFieldsInputGPS(paramsGPS);
		BULMAFieldsInputShape paramsDatasetShape = new BULMAFieldsInputShape(paramsShapes);

		return generateDataFrames(datasetShapesFile, datasetGPSFile, paramsDatasetGPS, paramsDatasetShape,
				minPartitions, spark);
	}

	/**
	 * Generates DataFrames
	 */
	@SuppressWarnings("serial")
	public static Dataset<Tuple2<String, GeoLine>> generateDataFrames(Dataset<Row> shapeFileDS, Dataset<Row> gpsFileDS,
			BULMAFieldsInputGPS paramsDatasetGPS, BULMAFieldsInputShape paramsDatasetShape, Integer minPartitions,
			SparkSession spark) throws Exception {

		gpsFileDS = removeHeaderGPS(gpsFileDS, paramsDatasetGPS);
		shapeFileDS = removeHeaderShape(shapeFileDS, paramsDatasetShape);

		JavaRDD<Row> gpsString = gpsFileDS.toJavaRDD();
		JavaRDD<Row> shapeString = shapeFileDS.toJavaRDD();

		JavaPairRDD<String, Iterable<GeoPoint>> rddGPSPointsPair = gpsString
				.mapToPair(new PairFunction<Row, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(Row s) throws Exception {
						GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(s.getString(0), arrayIndexFieldsInputGPS,
								FILE_SEPARATOR);
						return new Tuple2<String, GeoPoint>(gpsPoint.getBusCode(), gpsPoint);

					}
				}).groupByKey(minPartitions);

		JavaPairRDD<String, Iterable<GeoPoint>> rddShapePointsPair = shapeString
				.mapToPair(new PairFunction<Row, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(Row s) throws Exception {
						ShapePoint shapePoint = ShapePoint.createShapePointRoute(s.getString(0),
								arrayIndexFieldsInputShape, FILE_SEPARATOR);
						return new Tuple2<String, GeoPoint>(shapePoint.getId(), shapePoint);
					}
				}).groupByKey(minPartitions);

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
						Float distanceTraveled = lastPoint.getDistanceTraveled();
						String route = lastPoint.getRoute();
						GeoLine geoLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								listGeoPoint, route, greaterDistance);

						return new Tuple2<String, GeoLine>(lineBlockingKey, geoLine);
					}
				});

		JavaPairRDD<String, GeoLine> union = rddGPSLinePair.union(rddShapeLinePair);

		Encoder<GeoLine> geoLineEncoder = Encoders.javaSerialization(GeoLine.class);

		return spark.createDataset(JavaPairRDD.toRDD(union), Encoders.tuple(Encoders.STRING(), geoLineEncoder));
	}

	private static Dataset<Row> removeHeaderGPS(Dataset<Row> dataset, BULMAFieldsInputGPS fieldsInputGPS) {
		Row header = dataset.first();
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean call(Row value) throws Exception {
				if (value.equals(header)) {

					String[] fields = value.getString(0).split(FILE_SEPARATOR);

					for (int i = 0; i < NUMBER_ATTRIBUTES_INPUTS; i++) {

						if (fields[i].equals(fieldsInputGPS.getBusCode())) {
							arrayIndexFieldsInputGPS[0] = i;
						} else if (fields[i].equals(fieldsInputGPS.getLatitude())) {
							arrayIndexFieldsInputGPS[1] = i;
						} else if (fields[i].equals(fieldsInputGPS.getLongitude())) {
							arrayIndexFieldsInputGPS[2] = i;
						} else if (fields[i].equals(fieldsInputGPS.getTimestamp())) {
							arrayIndexFieldsInputGPS[3] = i;
						} else if (fields[i].equals(fieldsInputGPS.getLineCode())) {
							arrayIndexFieldsInputGPS[4] = i;
						} else if (fields[i].equals(fieldsInputGPS.getGpsId())) {
							arrayIndexFieldsInputGPS[5] = i;
						} else {
							throw new Exception("Input fields do not match GPS file fields.");
						}
					}

					return false;

				}
				return true;
			}
		});

		return dataset;
	}

	private static Dataset<Row> removeHeaderShape(Dataset<Row> dataset, BULMAFieldsInputShape fieldsInputShape) {
		Row header = dataset.first();
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean call(Row value) throws Exception {
				if (value.equals(header)) {

					String[] fields = value.getString(0).split(FILE_SEPARATOR);

					for (int i = 0; i < NUMBER_ATTRIBUTES_INPUTS; i++) {
						if (fields[i].equals(fieldsInputShape.getRoute())) {
							arrayIndexFieldsInputShape[0] = i;
						} else if (fields[i].equals(fieldsInputShape.getShapeId())) {
							arrayIndexFieldsInputShape[1] = i;
						} else if (fields[i].equals(fieldsInputShape.getLatitude())) {
							arrayIndexFieldsInputShape[2] = i;
						} else if (fields[i].equals(fieldsInputShape.getLongitude())) {
							arrayIndexFieldsInputShape[3] = i;
						} else if (fields[i].equals(fieldsInputShape.getSequence())) {
							arrayIndexFieldsInputShape[4] = i;
						} else if (fields[i].equals(fieldsInputShape.getDistanceTraveled())) {
							arrayIndexFieldsInputShape[5] = i;
						} else {
							throw new Exception("Input fields do not match shape file fields.");
						}
					}

					return false;
				}

				return true;
			}
		});

		return dataset;
	}

	@SuppressWarnings("serial")
	public static Dataset<String> run(Dataset<Tuple2<String, GeoLine>> lines, Integer minPartitions, SparkSession spark)
			throws Exception {

		JavaRDD<Tuple2<String, GeoLine>> rddUnionLines = lines.toJavaRDD();

		JavaPairRDD<String, Iterable<GeoLine>> rddGroupedUnionLines = rddUnionLines
				.mapToPair(new PairFunction<Tuple2<String, GeoLine>, String, GeoLine>() {

					@Override
					public Tuple2<String, GeoLine> call(Tuple2<String, GeoLine> t) throws Exception {

						return new Tuple2<String, GeoLine>(t._1, t._2);
					}
				}).groupByKey(minPartitions);

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
													|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME) {
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

		JavaRDD<List<GPSLine>> rddClosestPoint = rddSetUpTrips.map(new Function<List<GPSLine>, List<GPSLine>>() {

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
									((GPSPoint) gpsPoint).setThresholdShape(trip.getShapeLine().getThresholdDistance());
								}
							}
						}
					}
				}
				return entry;
			}
		});

		JavaRDD<String> rddOutput = rddClosestPoint.flatMap(new FlatMapFunction<List<GPSLine>, String>() {

			@Override
			public Iterator<String> call(List<GPSLine> listGPS) throws Exception {
				List<String> listOutput = new ArrayList<>();
				for (GPSLine gpsLine : listGPS) {
					String stringOutput = "";
					if (gpsLine != null) {

						if (gpsLine.getMapTrips().isEmpty()) {
							GPSPoint gpsPoint;
							for (GeoPoint geoPoint : gpsLine.getListGeoPoints()) {
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

								stringOutput += Problem.NO_SHAPE.getCode() + "/n";

							}
						}

						for (Integer key : gpsLine.getMapTrips().keySet()) {
							for (Trip trip : gpsLine.getTrip(key)) {

								for (GeoPoint geoPoint : trip.getGPSPoints()) {

									GPSPoint gpsPoint = (GPSPoint) geoPoint;

									stringOutput += key + FILE_SEPARATOR;
									stringOutput += gpsPoint.getLineCode() + FILE_SEPARATOR;
									if (trip.getShapeLine() == null) {
										stringOutput += "-" + FILE_SEPARATOR;
										stringOutput += "-" + FILE_SEPARATOR;
										stringOutput += "-" + FILE_SEPARATOR;
										stringOutput += "-" + FILE_SEPARATOR;
									} else {
										stringOutput += gpsPoint.getClosestPoint().getId() + FILE_SEPARATOR;
										stringOutput += gpsPoint.getClosestPoint().getPointSequence() + FILE_SEPARATOR;
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
										stringOutput += trip.getProblem().getCode() + "/n";
									} else if (gpsPoint.getDistanceClosestShapePoint() > gpsPoint.getThresholdShape()) {
										stringOutput += Problem.OUTLIER_POINT.getCode() + "/n";
									} else {
										stringOutput += trip.getProblem().getCode() + "/n";
									}
								}
							}
						}
					}

					listOutput.add(stringOutput);
				}
				return listOutput.iterator();
			}
		});

		return spark.createDataset(rddOutput.rdd(), Encoders.STRING());
	}

}