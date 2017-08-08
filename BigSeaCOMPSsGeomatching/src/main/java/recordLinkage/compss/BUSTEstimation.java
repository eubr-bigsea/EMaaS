package recordLinkage.compss;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import PointDependencies.Problem;
import PointDependencies.ShapePoint;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.storage.StorageManager;
import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.BulmaOutputGrouping;
import recordLinkage.dependencies.ShapeLine;
import scala.Tuple2;

public class BUSTEstimation {

	
	public static void main(String[] args) throws ParseException {

		Integer numPartitions = 4;
		String shapeSource = "/home/demo/compss_test/BUSTE_data/shapesGTFS.csv";
		String stopsSource = "/home/demo/compss_test/BUSTE_data/stopTime.csv";
		String gpsFolder = "/home/demo/compss_test/BulmaOutput/";
		boolean list = false;
		
		int argIndex = 0;
		while (argIndex < args.length) {

			String arg = args[argIndex++];
			if (arg.equals("-shape")) {
				shapeSource = args[argIndex++];

			} else if (arg.equals("-stops")) {
				stopsSource = args[argIndex++];

			} else if (arg.equals("-folder")) {
				gpsFolder = args[argIndex++];

				// f = number of partitions
			} else if (arg.equals("-f")) {
				numPartitions = Integer.parseInt(args[argIndex++]);
				
			} else if (arg.equals("-list")) {
				list = true;
			}
		}

		HashMap<String, LinkedList<ShapePoint>> shapePair = mapShape(shapeSource);
		HashMap<String, ShapeLine> groupedShape = groupShape(shapePair);
		HashMap<String, String> mapStopPoints = mapBusStops(stopsSource);
		
		LinkedList<String> results = new LinkedList<String>();

		for (int i = 0; i < numPartitions; i++) {
			String filePath = gpsFolder + String.format("%02d", i) + ".csv";
			HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput = mapBulmaOutput(filePath);
			System.out.println(filePath);
			HashMap<String, BulmaOutputGrouping> groupedOutput = groupBulmaOutput(partialBulmaOutput);
			results = mergeResults(groupedOutput, groupedShape, mapStopPoints, results);

		}
		System.out.println("[LOG] Result size = " + results.size());

		if (list) {
			for (String result : results) {
				System.out.println(result);
			}
		}

	}

	public static HashMap<String, ShapeLine> groupShape(HashMap<String, LinkedList<ShapePoint>> shapePair) {
		HashMap<String, ShapeLine> output = new HashMap<String, ShapeLine>();

		for (Entry<String, LinkedList<ShapePoint>> entry : shapePair.entrySet()) {
			LinkedList<ShapePoint> listShapePoints = entry.getValue();
			String route = listShapePoints.get(listShapePoints.size() - 1).getRoute();
			ShapeLine shapeLine = new ShapeLine(entry.getKey(), listShapePoints, route);
			output.put(entry.getKey(), shapeLine);
		}

		return output;
	}

	public static HashMap<String, BulmaOutputGrouping> groupBulmaOutput(
			HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput) {

		HashMap<String, BulmaOutputGrouping> output = new HashMap<String, BulmaOutputGrouping>();

		for (Entry<String, LinkedList<BulmaOutput>> entry : partialBulmaOutput.entrySet()) {
			String key = entry.getKey().split("\\:")[0];
			HashMap<String, BulmaOutput> mapGrouping = new HashMap<String, BulmaOutput>();

			for (BulmaOutput bulmaOutput : entry.getValue()) {
				mapGrouping.put(bulmaOutput.getShapeSequence(), bulmaOutput);
			}

			output.put(key, new BulmaOutputGrouping(mapGrouping));
		}
		return output;
	}
	
	public static HashMap<String, LinkedList<ShapePoint>> mapShape(String filePath) {

		HashMap<String, LinkedList<ShapePoint>> output = new HashMap<String, LinkedList<ShapePoint>>();

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

			ShapePoint shapePoint = new ShapePoint(route, shapeId, latShape, lonShape, shapeSequence, distTraveled);

			if (!output.containsKey(shapePoint.getId())) {
				output.put(shapePoint.getId(), new LinkedList<ShapePoint>());
			}
			output.get(shapePoint.getId()).add(shapePoint);
		}

		return output;
	}
	
	public static HashMap<String, String> mapBusStops(String filePath) {

		HashMap<String, String> output = new HashMap<String, String>();

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
			String stopId = data.get("stop_id").toString();
			String shapeId = data.get("shape_id").toString();
			String shapeSequence = data.get("closest_shape_point").toString();

			output.put(shapeSequence, stopId);
		}

		return output;
	}
	
	public static HashMap<String, LinkedList<BulmaOutput>> mapBulmaOutput(String filePath) {
		HashMap<String, LinkedList<BulmaOutput>> output = new HashMap<String, LinkedList<BulmaOutput>>();

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

			String tripNum = data.get("TRIP_NUM").toString();
			String route = data.get("ROUTE").toString();
			String shapeId = data.get("SHAPE_ID").toString();
			String shapeSequence = data.get("SHAPE_SEQ").toString();
			String latShape = data.get("LAT_SHAPE").toString();
			String lonShape = data.get("LON_SHAPE").toString();
			String gpsPointId = data.get("GPS_POINT_ID").toString();
			String busCode = data.get("BUS_CODE").toString();
			String timestamp = data.get("TIMESTAMP").toString();
			String latGPS = data.get("LAT_GPS").toString();
			String lonGPS = data.get("LON_GPS").toString();
			String dinstance = data.get("DISTANCE").toString();
			String thresholdProblem = data.get("THRESHOLD_PROBLEM").toString();
			String tripProblem = data.get("TRIP_PROBLEM").toString();

			BulmaOutput bulmaOutput = new BulmaOutput(tripNum, route, shapeId, shapeSequence, latShape, lonShape,
					gpsPointId, busCode, timestamp, latGPS, lonGPS, dinstance, thresholdProblem, tripProblem);

			String key = bulmaOutput.getShapeId() + ":" + bulmaOutput.getBusCode() + ":" + bulmaOutput.getTripNum();

			if (!output.containsKey(key)) {
				output.put(key, new LinkedList<BulmaOutput>());
			}
			output.get(key).add(bulmaOutput);
			
		}
		
		return output;
	}
	
	public static LinkedList<String> mergeResults(HashMap<String, BulmaOutputGrouping> groupedOutput,
			HashMap<String, ShapeLine> groupedShape, HashMap<String, String> mapStopPoints, LinkedList<String> results) throws ParseException {

		for (Entry<String, BulmaOutputGrouping> entry : groupedOutput.entrySet()) {

			String currentKey = entry.getKey();
			BulmaOutputGrouping bulmaOutputGrouping = entry.getValue();
			ShapeLine currentShapeLine = groupedShape.get(currentKey);

			if (currentShapeLine != null) {

				Tuple2<Float, String> previousGPSPoint = null;
				Tuple2<Float, String> nextGPSPoint = null;
				LinkedList<Integer> pointsBetweenGPS = new LinkedList<Integer>();

				for (int i = 0; i < currentShapeLine.getListGeoPoint().size(); i++) {
					ShapePoint currentShapePoint = currentShapeLine.getListGeoPoint().get(i);
					String currentShapeSequence = currentShapePoint.getPointSequence();

					String currentTimestamp;
					if (previousGPSPoint == null) {
						if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {

							currentTimestamp = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence)
									.getTimestamp();
							previousGPSPoint = new Tuple2<Float, String>(currentShapePoint.getDistanceTraveled(),
									currentTimestamp);
							String buCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence)
									.getBusCode();
							String problemCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence)
									.getTripProblem();
							addOutput(currentShapeLine.getShapeId(), currentShapeSequence, buCode, currentTimestamp,
									problemCode, results, mapStopPoints);

						} else {
							addOutput(currentShapeLine.getShapeId(), "-", currentShapeSequence, "-", "-", results,
									mapStopPoints);
						}
					} else {

						if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {
							String busCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence)
									.getBusCode();
							String problemCode = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence)
									.getTripProblem();
							currentTimestamp = bulmaOutputGrouping.getMapOutputGrouping().get(currentShapeSequence)
									.getTimestamp();
							nextGPSPoint = new Tuple2<Float, String>(currentShapePoint.getDistanceTraveled(),
									currentTimestamp);

							generateOutputFromPointsInBetween(currentShapeLine.getShapeId(), previousGPSPoint,
									pointsBetweenGPS, nextGPSPoint, currentShapeLine.getListGeoPoint(), busCode,
									results, mapStopPoints);

							addOutput(currentShapeLine.getShapeId(), currentShapeSequence, busCode, currentTimestamp,
									problemCode, results, mapStopPoints);

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
						String currentShapeSequence = currentShapeLine.getListGeoPoint().get(indexPointsInBetween)
								.getPointSequence();
						addOutput(currentShapeLine.getShapeId(), "-", currentShapeSequence, "-", "-", results,
								mapStopPoints);

					}
				}
			}

		}
		return results;
	}

	public static void addOutput(String shapeId, String currentShapeSequence, String busCode, String currentTimestamp,
			String problemCode, LinkedList<String> listOutput, HashMap<String, String> mapStopPoints) {
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

		listOutput.add(shapeId + "," + currentShapeSequence + "," + busCode + "," + currentTimestamp + "," + stopPointId
				+ "," + problem);
	}
	
	public static void generateOutputFromPointsInBetween(String shapeId, Tuple2<Float, String> previousGPSPoint,
			LinkedList<Integer> pointsBetweenGPS, Tuple2<Float, String> nextGPSPoint, LinkedList<ShapePoint> listGeoPointsShape,
			String busCode, LinkedList<String> listOutput, HashMap<String, String> mapStopPoints) throws ParseException {

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
		for (Integer indexPointsInBetween : pointsBetweenGPS) {

			currentDistanceTraveled = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled()
					- previousDistanceTraveled;
			generatedTimeDifference = (long) ((currentDistanceTraveled * time) / distanceTraveled);
			generatedTime = previousTime + generatedTimeDifference;
			generatedTimeString = getTimeString(generatedTime);
			sequence = listGeoPointsShape.get(indexPointsInBetween).getPointSequence();

			addOutput(shapeId, sequence, busCode, generatedTimeString, "-", listOutput, mapStopPoints);

		}

	}

	public static String getTimeString(long generatedTime) {
		Date date = new Date(generatedTime);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		return formatter.format(date);
	}
	
	public static long getTimeLong(String timestamp) throws ParseException {
		SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
		return parser.parse(timestamp).getTime();
	}
}
