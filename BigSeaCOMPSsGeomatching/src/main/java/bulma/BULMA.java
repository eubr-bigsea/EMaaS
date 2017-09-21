package bulma;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import bulma.dependencies.GPSLine;
import bulma.dependencies.GPSPoint;
import bulma.dependencies.GeoPoint;
import bulma.dependencies.PossibleShape;
import bulma.dependencies.Problem;
import bulma.dependencies.ShapeLine;
import bulma.dependencies.ShapePoint;
import bulma.dependencies.Trip;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.storage.StorageManager;
import scala.Tuple2;

public class BULMA {

	@SuppressWarnings("deprecation")
	private static GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();
	private static final double THRESHOLD_TIME = 600000; // 20 minutes
	private static final double PERCENTAGE_DISTANCE = 0.09;
	private static final String FILE_SEPARATOR = ",";

	public static void main(String[] args) throws Exception {

		String shapeFile = "bus_data/gtfsFiles/shapesGTFS.csv";
		String gpsFile = "bus_data/gtfsFiles/00.csv";
		String gpsTmp = gpsFile.substring(0, gpsFile.lastIndexOf("/") + 1) + "_";
		int numPartitions = 2;
		boolean list = true;

		int argIndex = 0;
		while (argIndex < args.length) {

			String arg = args[argIndex++];
			if (arg.equals("-shape")) {
				shapeFile = args[argIndex++];

			} else if (arg.equals("-gps")) {
				gpsFile = args[argIndex++];
				gpsTmp = gpsFile.substring(0, gpsFile.lastIndexOf("/") + 1) + "_";

			} else if (arg.equals("-partitions")) {
				numPartitions = Integer.parseInt(args[argIndex++]);

			} else if (arg.equals("-list")) {
				list = true;
			}
		}

		splitFile(numPartitions, gpsFile, gpsTmp);

		LinkedList<String> results = new LinkedList<String>();
		HashMap<String, LinkedList<GeoPoint>> mapShape = mapShape(shapeFile);
		HashMap<String, LinkedList<ShapeLine>> groupedShape = groupShape(mapShape);

		for (int i = 0; i < numPartitions; i++) {
			String gpsPath = gpsTmp + String.format("%02d", i) + ".csv";
			HashMap<String, LinkedList<GeoPoint>> mapGPS = mapGPSFileSplitted(gpsPath);
			HashMap<String, LinkedList<GPSLine>> groupedGPS = groupGPSFile(mapGPS);
			LinkedList<GPSLine> possibleShapes = mapPossibleShapes(groupedGPS, groupedShape);
			LinkedList<GPSLine> trueShapes = getTrueShapes(possibleShapes);
			LinkedList<GPSLine> closestPoints = getClosestPoints(trueShapes);
			results = generateOutput(closestPoints, results);
		}

		removeTmpFiles(numPartitions, gpsTmp);

		if (list) {
			for (String result : results) {
				System.out.println(result);
			}
		}
		System.out.println("[LOG] Result size = " + results.size());

	}

	private static void removeTmpFiles(int numPartitions, String gpsTmp) {
		File file;
		for (int i = 0; i < numPartitions; i++) {
			String filePath = gpsTmp + String.format("%02d", i) + ".csv";
			file = new File(filePath);
			if (!file.delete()) {
				System.err.println("Error deleting file: " + filePath);
			}
		}

	}

	private static void splitFile(int numPartitions, String gpsFile, String gpsTmp)
			throws FileNotFoundException, UnsupportedEncodingException {
		HashMap<String, LinkedList<GeoPoint>> map = mapGPSFile(gpsFile);

		for (int i = 0; i < numPartitions; i++) {
			PrintWriter writer = new PrintWriter(gpsTmp + String.format("%02d", i) + ".csv", "UTF-8");
			writer.println("bus.code,latitude,longitude,timestamp,line.code,gps.id");

			int nextIndex = i;
			int j = 0;
			for (Entry<String, LinkedList<GeoPoint>> entrySet : map.entrySet()) {

				if (j == nextIndex) {
					nextIndex += numPartitions;

					for (GeoPoint geoPoint : entrySet.getValue()) {
						writer.println(geoPoint.toString());
					}
				}
				j++;
			}
			writer.close();
		}

	}

	public static HashMap<String, LinkedList<GeoPoint>> mapGPSFile(String filePath) {

		HashMap<String, LinkedList<GeoPoint>> output = new HashMap<String, LinkedList<GeoPoint>>();
		DataSource dataSourceOSM = AbstractExec.getDataCSV(filePath, ',');

		StorageManager storageOSM = new StorageManager();

		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
		storageOSM.enableInMemoryProcessing();
		// adds the "data" to the algorithm
		storageOSM.addDataSource(dataSourceOSM);

		if (!storageOSM.isDataExtracted()) {
			storageOSM.extractData();
		}

		for (GenericObject genericObj : storageOSM.getExtractedData()) {

			JsonRecord data = genericObj.getData();

			String route = data.get("line.code").toString();
			String gpsPointId = data.get("gps.id").toString();
			String busCode = data.get("bus.code").toString();
			String timestamp = data.get("timestamp").toString();
			String latitude = data.get("latitude").toString();
			String longitude = data.get("longitude").toString();

			GPSPoint gpsPoint = new GPSPoint(busCode, latitude, longitude, timestamp, route, gpsPointId);

			if (!output.containsKey(busCode)) {
				output.put(busCode, new LinkedList<GeoPoint>());
			}
			output.get(busCode).add(gpsPoint);
		}

		return output;
	}

	public static HashMap<String, LinkedList<GeoPoint>> mapShape(String filePath) {

		HashMap<String, LinkedList<GeoPoint>> output = new HashMap<String, LinkedList<GeoPoint>>();

		DataSource dataSourceOSM = AbstractExec.getDataCSV(filePath, ',');

		StorageManager storageOSM = new StorageManager();

		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
		storageOSM.enableInMemoryProcessing();
		// adds the "data" to the algorithm
		storageOSM.addDataSource(dataSourceOSM);

		if (!storageOSM.isDataExtracted()) {
			storageOSM.extractData();
		}

		for (GenericObject genericObj : storageOSM.getExtractedData()) {
			JsonRecord data = genericObj.getData();
			String route = data.get("route_id").toString();
			String shapeId = data.get("shape_id").toString();
			String latShape = data.get("shape_pt_lat").toString();
			String lonShape = data.get("shape_pt_lon").toString();
			String shapeSequence = data.get("shape_pt_sequence").toString();
			String distTraveled = data.get("shape_dist_traveled").toString();

			GeoPoint shapePoint = new ShapePoint(route, shapeId, latShape, lonShape, shapeSequence, distTraveled);

			if (!output.containsKey(shapeId)) {
				output.put(shapeId, new LinkedList<GeoPoint>());
			}
			output.get(shapeId).add(shapePoint);
		}

		return output;
	}

	public static HashMap<String, LinkedList<ShapeLine>> groupShape(HashMap<String, LinkedList<GeoPoint>> shapePair) {

		@SuppressWarnings("deprecation")
		GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();

		HashMap<String, LinkedList<ShapeLine>> output = new HashMap<String, LinkedList<ShapeLine>>();

		for (Entry<String, LinkedList<GeoPoint>> entry : shapePair.entrySet()) {
			LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>();
			Double latitude;
			Double longitude;
			ShapePoint lastPoint = null;
			String lineBlockingKey = null;
			float greaterDistance = 0;

			LinkedList<GeoPoint> listGeoPoint = entry.getValue();
			for (int i = 0; i < listGeoPoint.size(); i++) {
				GeoPoint currentGeoPoint = listGeoPoint.get(i);
				if (i < listGeoPoint.size() - 1) {
					float currentDistance = GeoPoint.getDistanceInMeters(currentGeoPoint, listGeoPoint.get(i + 1));
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
			ShapeLine shapeLine = new ShapeLine(entry.getKey(), lineString, distanceTraveled, lineBlockingKey,
					listGeoPoint, route, greaterDistance);

			if (!output.containsKey(route)) {
				output.put(route, new LinkedList<ShapeLine>());
			}
			output.get(route).add(shapeLine);
		}

		return output;
	}

	public static HashMap<String, LinkedList<GeoPoint>> mapGPSFileSplitted(String filePath) {

		HashMap<String, LinkedList<GeoPoint>> output = new HashMap<String, LinkedList<GeoPoint>>();
		DataSource dataSourceOSM = AbstractExec.getDataCSV(filePath, ',');

		StorageManager storageOSM = new StorageManager();

		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
		storageOSM.enableInMemoryProcessing();
		// adds the "data" to the algorithm
		storageOSM.addDataSource(dataSourceOSM);

		if (!storageOSM.isDataExtracted()) {
			storageOSM.extractData();
		}

		for (GenericObject genericObj : storageOSM.getExtractedData()) {

			JsonRecord data = genericObj.getData();

			String route = data.get("line.code").toString();
			String gpsPointId = data.get("gps.id").toString();
			String busCode = data.get("bus.code").toString();
			String timestamp = data.get("timestamp").toString();
			String latitude = data.get("latitude").toString();
			String longitude = data.get("longitude").toString();

			GPSPoint gpsPoint = new GPSPoint(busCode, latitude, longitude, timestamp, route, gpsPointId);

			if (!output.containsKey(busCode)) {
				output.put(busCode, new LinkedList<GeoPoint>());
			}
			output.get(busCode).add(gpsPoint);
		}

		return output;
	}

	public static HashMap<String, LinkedList<GPSLine>> groupGPSFile(
			HashMap<String, LinkedList<GeoPoint>> mapGPSPoints) {

		HashMap<String, LinkedList<GPSLine>> output = new HashMap<String, LinkedList<GPSLine>>();

		for (Entry<String, LinkedList<GeoPoint>> entrySet : mapGPSPoints.entrySet()) {

			LinkedList<Coordinate> coordinates = new LinkedList<>();
			Double latitude;
			Double longitude;
			String lineBlockingKey = null;
			float greaterDistance = 0;

			LinkedList<GeoPoint> listGeoPoint = entrySet.getValue();
			for (int i = 0; i < listGeoPoint.size(); i++) {
				GeoPoint currentGeoPoint = listGeoPoint.get(i);
				if (i < listGeoPoint.size() - 1) {
					float currentDistance = GeoPoint.getDistanceInMeters(currentGeoPoint, listGeoPoint.get(i + 1));
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
			GPSLine gpsLine = null;

			if (array.length > 1 && lineBlockingKey != null) {
				LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
				gpsLine = new GPSLine(entrySet.getKey(), lineString, lineBlockingKey, listGeoPoint, greaterDistance);
			} else if (array.length >= 1) {
				gpsLine = new GPSLine(entrySet.getKey(), null, "REC", listGeoPoint, greaterDistance);
			}

			if (!output.containsKey(lineBlockingKey)) {
				output.put(lineBlockingKey, new LinkedList<GPSLine>());
			}
			output.get(lineBlockingKey).add(gpsLine);

		}

		return output;

	}

	public static LinkedList<GPSLine> mapPossibleShapes(HashMap<String, LinkedList<GPSLine>> groupedGPS,
			HashMap<String, LinkedList<ShapeLine>> groupedShape) throws ParseException {

		LinkedList<GPSLine> output = new LinkedList<GPSLine>();

		for (Entry<String, LinkedList<GPSLine>> entryGPS : groupedGPS.entrySet()) {

			LinkedList<ShapeLine> shapeLineList = groupedShape.get(entryGPS.getKey());
			LinkedList<GPSLine> gpsLineList = entryGPS.getValue();

			if (shapeLineList != null) {

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
									possibleShape
											.addPossibleFirstPoint(new Tuple2<String, Integer>(blockingKeyFromTime, i));

								} else if (currentDistanceToEndPoint < thresholdDistanceCurrentShape) {

									if (blockingKeyFromTime == null
											|| currentPoint.getTime() - timePreviousPointGPS > THRESHOLD_TIME) {
										if (i > lastIndexEnd + 1) {
											blockingKeyFromTime = currentPoint.getBlockingKeyFromTime();
										}
										lastIndexEnd = i;
									}

									timePreviousPointGPS = currentPoint.getTime();
									possibleShape
											.addPossibleLastPoint(new Tuple2<String, Integer>(blockingKeyFromTime, i));

								}
							}
						}
						gpsLine.addPossibleShapeLine(possibleShape);
					}
				}
			}
			output.addAll(gpsLineList);
		}

		return output;

	}

	public static LinkedList<GPSLine> getTrueShapes(LinkedList<GPSLine> gpsPlusPossibleShapes) throws ParseException {

		Queue<Tuple2<String, Integer>> firstGPSPoints;
		Queue<Tuple2<String, Integer>> lastGPSPoints;

		for (GPSLine gpsLine : gpsPlusPossibleShapes) {
			boolean hasRoundShape = false;

			if (gpsLine.getListPossibleShapeLines() == null) {
				return gpsPlusPossibleShapes;
			}
			for (PossibleShape possibleShape : gpsLine.getListPossibleShapeLines()) {
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
			Collections.sort(gpsLine.getListPossibleShapeLines());
			if (!hasRoundShape && gpsLine.getListPossibleShapeLines().size() > 2) {
				gpsLine.findBestShapes();
			} else if (hasRoundShape && gpsLine.getListPossibleShapeLines().size() > 1) {
				gpsLine.findBestShape();
			}
			gpsLine.setUpTrips();
		}
		return gpsPlusPossibleShapes;
	}

	public static LinkedList<GPSLine> getClosestPoints(LinkedList<GPSLine> gpsPlusTrueShapes) throws Exception {
		for (GPSLine gpsLine : gpsPlusTrueShapes) {

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
		return gpsPlusTrueShapes;

	}

	public static LinkedList<String> generateOutput(LinkedList<GPSLine> closestPoints, LinkedList<String> results)
			throws Exception {
		for (GPSLine gpsLine : closestPoints) {
			if (gpsLine != null) {

				if (gpsLine.getMapTrips().isEmpty()) {
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
						results.add(stringOutput);

					}
				}

				for (Integer key : gpsLine.getMapTrips().keySet()) {
					for (Trip trip : gpsLine.getTrip(key)) {

						for (GeoPoint geoPoint : trip.getGPSPoints()) {
							String stringOutput = "";
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
								stringOutput += trip.getProblem().getCode();

							} else if (gpsPoint.getDistanceClosestShapePoint() > gpsPoint.getThresholdShape()) {
								stringOutput += Problem.OUTLIER_POINT.getCode();
							} else {
								stringOutput += trip.getProblem().getCode();
							}
							results.add(stringOutput);
						}
					}
				}
			}
		}
		return results;

	}
}
