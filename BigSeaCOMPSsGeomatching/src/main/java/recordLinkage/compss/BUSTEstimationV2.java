package recordLinkage.compss;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
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
import recordLinkage.dependencies.ComparatorTmpOutput;
import recordLinkage.dependencies.ShapeLine;
import recordLinkage.dependencies.TicketInformation;
import scala.Tuple2;
import scala.Tuple3;

/**
 * (COMPSs implementation version)
 * 
 * This class does the post-processing of Bulma, get its output and
 * interpolates the shape file. Besides, this class add the stop points file to
 * the output. In this version (V3), this class includes the bus tickets file to
 * the output.
 * 
 * @author Andreza
 *
 */
public class BUSTEstimationV2 {

	private static final String SEPARATOR = ",";
	
	public static void main(String[] args) throws ParseException, FileNotFoundException, UnsupportedEncodingException {

		Integer numPartitions = 2;
		String shapeSource = "bus_data/gtfsFiles/shapesGTFS.csv";
		String stopsSource = "bus_data/gtfsFiles/stopTime.csv";
		String bulmaOutputDirectory = "bus_data/outputBulma/";
		String ticketsDirectory = "bus_data/gtfsFiles/";
		boolean list = false;
		
		int argIndex = 0;
		while (argIndex < args.length) {

			String arg = args[argIndex++];
			if (arg.equals("-shape")) {
				shapeSource = args[argIndex++];

			} else if (arg.equals("-stops")) {
				stopsSource = args[argIndex++];

			} else if (arg.equals("-tickets")) {
				ticketsDirectory = args[argIndex++];
				
			} else if (arg.equals("-bo")) {
				bulmaOutputDirectory = args[argIndex++];
				
			} else if (arg.equals("-partitions")) {
				numPartitions = Integer.parseInt(args[argIndex++]);

			} else if (arg.equals("-list")) {
				list = true;
			}
		}
		
		HashMap<String, LinkedList<ShapePoint>> shapePair = mapShape(shapeSource);
		HashMap<String, ShapeLine> groupedShape = groupShape(shapePair);
		HashMap<String, HashMap<String, String>> mapStopPoints = mapBusStops(stopsSource);
		LinkedList<String> results = new LinkedList<String>();
		
		for (int i = 0; i < numPartitions; i++) {
			System.out.println(i);
			String filePath = bulmaOutputDirectory + "/_bo" + String.format("%02d", i) + ".csv";
			HashMap<String, LinkedList<TicketInformation>> tickets = mapTicketsSplitted(ticketsDirectory + "/_ticket"  + String.format("%02d", i) + ".csv");
			HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput = mapBulmaOutputSplitted(filePath);
			LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergedOutput = mergeInputs(
					partialBulmaOutput, groupedShape, mapStopPoints);
			HashMap<String, LinkedList<String>> tmpOutput = generateTmpOutput(mergedOutput);
			results = insertTicketsInformation(tmpOutput, tickets, results);

		}
		System.out.println("[LOG] Result size = " + results.size());

		if (list) {
			for (String result : results) {
				System.out.println(result);
			}
		} 
	}
	
	public static LinkedList<String> insertTicketsInformation (HashMap<String, LinkedList<String>> listOutput, HashMap<String, LinkedList<TicketInformation>> tickets, LinkedList<String> results) throws ParseException {
		
		for (Entry<String, LinkedList<String>> entrySet: listOutput.entrySet()) {
			String currentBusCode = entrySet.getKey();
			String nextTimeString = null; 
			
			LinkedList<String> listValues = entrySet.getValue();
			Collections.sort(listValues, new ComparatorTmpOutput());
			
			for (int i = listValues.size()-1; i >= 0; i--) {
				String currentString = listValues.get(i); 
				String currentBusStop = currentString.split(SEPARATOR)[13];
				
				if (!currentBusStop.equals("-")) {
					String currentTimeString = currentString.split(SEPARATOR)[12];
					if (!currentTimeString.equals("-")) {
						if (nextTimeString == null) {
							nextTimeString = currentTimeString;
							results.add(0, currentString + SEPARATOR + 0);
							
						} else {
							int number = getNumberTicketsOfBusStop(currentBusCode, currentTimeString, nextTimeString, tickets);
							nextTimeString = currentTimeString;
							results.add(0, currentString + SEPARATOR + number);
						}
					} else {
						results.add(0, currentString + SEPARATOR + "-");
					}
					
				} else {
					results.add(0, currentString + SEPARATOR + "-");
				}
			}
		}

		return results;
	}

	public static HashMap<String, LinkedList<ShapePoint>> mapShape(String shapeSource) {

		HashMap<String, LinkedList<ShapePoint>> output = new HashMap<String, LinkedList<ShapePoint>>();

		DataSource dataSourceOSM = AbstractExec.getDataCSV(shapeSource, ',');

		StorageManager storageOSM = new StorageManager();
		storageOSM.enableInMemoryProcessing();
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

	public static HashMap<String, HashMap<String, String>> mapBusStops(String filePath) {

		HashMap<String, HashMap<String, String>> output = new HashMap<String, HashMap<String, String>>();

		DataSource dataSourceOSM = AbstractExec.getDataCSV(filePath, ',');

		StorageManager storageOSM = new StorageManager();
		storageOSM.enableInMemoryProcessing();
		storageOSM.addDataSource(dataSourceOSM);

		if (!storageOSM.isDataExtracted()) {
			storageOSM.extractData();
		}

		for (GenericObject genericObj : storageOSM.getExtractedData()) {
			JsonRecord data = genericObj.getData();
			String stopId = data.get("stop_id").toString();
			String shapeId = data.get("shape_id").toString();
			String shapeSequence = data.get("closest_shape_point").toString();

			if (!output.containsKey(shapeId)) {
				output.put(shapeId, new HashMap<String, String>());
			}

			output.get(shapeId).put(shapeSequence, stopId);
		}

		return output;
	}

	
	
	public static HashMap<String, LinkedList<BulmaOutput>> mapBulmaOutputSplitted(String filePath) {
		HashMap<String, LinkedList<BulmaOutput>> output = new HashMap<String, LinkedList<BulmaOutput>>();

		DataSource dataSourceOSM = AbstractExec.getDataCSV(filePath, ',');

		StorageManager storageOSM = new StorageManager();
		storageOSM.enableInMemoryProcessing();
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
	
	
	public static HashMap<String, LinkedList<TicketInformation>> mapTicketsSplitted(String filePath) {

		HashMap<String, LinkedList<TicketInformation>> output = new HashMap<String, LinkedList<TicketInformation>>();
		
		DataSource dataSourceOSM = AbstractExec.getDataCSV(filePath, ',');

		StorageManager storageOSM = new StorageManager();
		storageOSM.enableInMemoryProcessing();
		storageOSM.addDataSource(dataSourceOSM);

		if (!storageOSM.isDataExtracted()) {
			storageOSM.extractData();
		}

		for (GenericObject genericObj : storageOSM.getExtractedData()) {

			JsonRecord data = genericObj.getData();

			String codLine = data.get("CODLINHA").toString();
			String nameLine = data.get("NOMELINHA").toString();
			String busCode = data.get("CODVEICULO").toString();
			String ticketNumber = data.get("NUMEROCARTAO").toString();
			String timeOfUse = data.get("HORAUTILIZACAO").toString();
			String dateOfUse = data.get("DATAUTILIZACAO").toString();
			String birthDate = data.get("DATANASCIMENTO").toString();
			String gender = data.get("SEXO").toString();
			

			TicketInformation ticket = new TicketInformation(codLine, nameLine, busCode, ticketNumber, timeOfUse,
					dateOfUse, birthDate, gender);


			if (!output.containsKey(ticket.getBusCode())) {
				output.put(ticket.getBusCode(), new LinkedList<TicketInformation>());
			}
			output.get(ticket.getBusCode()).add(ticket);

		}
		return output;
	}
	
	public static LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergeInputs(
			HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput, HashMap<String, ShapeLine> groupedShape,
			HashMap<String, HashMap<String, String>> mapStopPoints) {

		LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> output = new LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>>();

		for (Entry<String, LinkedList<BulmaOutput>> entry : partialBulmaOutput.entrySet()) {
			String key = entry.getKey().split("\\:")[0];
			HashMap<String, BulmaOutput> mapGrouping = new HashMap<String, BulmaOutput>();

			for (BulmaOutput bulmaOutput : entry.getValue()) {
				mapGrouping.put(bulmaOutput.getShapeSequence(), bulmaOutput);
			}

			output.add(new Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>(
					new BulmaOutputGrouping(mapGrouping), groupedShape.get(key), mapStopPoints.get(key))); 
		}

		return output;
	}

	public static HashMap<String, LinkedList<String>> generateTmpOutput(
			LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergedOutput) throws ParseException {

		HashMap<String, LinkedList<String>> results = new HashMap<String, LinkedList<String>>();
		
		for (Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>> tuple : mergedOutput) {

			BulmaOutputGrouping bulmaOutputGrouping = tuple._1();
			ShapeLine currentShapeLine = tuple._2();
			HashMap<String, String> mapStopPoints = tuple._3();

			if (currentShapeLine != null) {

				Tuple2<Float, String> previousGPSPoint = null;
				Tuple2<Float, String> nextGPSPoint = null;
				LinkedList<Integer> pointsBetweenGPS = new LinkedList<Integer>();
				
				String tripNum = "-";

				for (int i = 0; i < currentShapeLine.getListGeoPoint().size(); i++) {
					ShapePoint currentShapePoint = currentShapeLine.getListGeoPoint().get(i);
					String currentShapeSequence = currentShapePoint.getPointSequence();
					String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();
					String currentShapeId = currentShapeLine.getShapeId();
					String currentLatShape = currentShapePoint.getLatitude();
					String currentLonShape = currentShapePoint.getLongitude();
					String currentRoute = currentShapeLine.getRoute();

					String currentTimestamp;

					if (previousGPSPoint == null) {
						if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {

							BulmaOutput currentOutput = bulmaOutputGrouping.getMapOutputGrouping()
									.get(currentShapeSequence);

							currentTimestamp = currentOutput.getTimestamp();
							previousGPSPoint = new Tuple2<Float, String>(
									currentShapePoint.getDistanceTraveled(), currentTimestamp);

							String busCode = currentOutput.getBusCode();
							String gpsPointId = currentOutput.getGpsPointId();
							String problemCode = currentOutput.getTripProblem();
							tripNum = currentOutput.getTripNum();
							String latGPS = currentOutput.getLatGPS();
							String lonGPS = currentOutput.getLonGPS();
							String distanceToShape = currentOutput.getDinstance();

							addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
									currentLatShape, currentLonShape, currentDistanceTraveled, busCode,
									gpsPointId, latGPS, lonGPS, distanceToShape, currentTimestamp,
									problemCode, results,mapStopPoints);

						} else {
							addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
									currentLatShape, currentLonShape, currentDistanceTraveled, "-", "-",
									"-", "-", "-", "-", "-", results, mapStopPoints);
						}
					} else {

						if (bulmaOutputGrouping.containsShapeSequence(currentShapeSequence)) {
							BulmaOutput currentOutput = bulmaOutputGrouping.getMapOutputGrouping()
									.get(currentShapeSequence);

							String busCode = currentOutput.getBusCode();
							String gpsPointId = currentOutput.getGpsPointId();
							String problemCode = currentOutput.getTripProblem();
							tripNum = currentOutput.getTripNum();
							String latGPS = currentOutput.getLatGPS();
							String lonGPS = currentOutput.getLonGPS();
							String distanceToShape = currentOutput.getDinstance();
							currentTimestamp = currentOutput.getTimestamp();

							nextGPSPoint = new Tuple2<Float, String>(
									currentShapePoint.getDistanceTraveled(), currentTimestamp);

							generateOutputFromPointsInBetween(currentShapeId, tripNum, previousGPSPoint,
									pointsBetweenGPS, nextGPSPoint, currentShapeLine.getListGeoPoint(), busCode,
									results, mapStopPoints);

							addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
									currentLatShape, currentLonShape, currentDistanceTraveled, busCode,
									gpsPointId, latGPS, lonGPS, distanceToShape, currentTimestamp,
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

						ShapePoint currentShapePoint = currentShapeLine.getListGeoPoint()
								.get(indexPointsInBetween);
						String currentShapeSequence = currentShapePoint.getPointSequence();
						String currentDistanceTraveled = currentShapePoint.getDistanceTraveled().toString();
						String currentShapeId = currentShapeLine.getShapeId();

						String currentLatShape = currentShapePoint.getLatitude();
						String currentLonShape = currentShapePoint.getLongitude();
						String currentRoute = currentShapeLine.getRoute();

						addOutput(currentRoute, tripNum, currentShapeId, currentShapeSequence,
								currentLatShape, currentLonShape, currentDistanceTraveled, "-", "-", "-",
								"-", "-", "-", "-", results, mapStopPoints);

					}
				}


				
			}

		}

		return results;
	}
	
	public static void addOutput(String route, String tripNum, String shapeId, String shapeSequence,
			String shapeLat, String shapeLon, String distanceTraveledShape, String busCode,
			String gpsPointId, String gpsLat, String gpsLon, String distanceToShapePoint,
			String timestamp, String problemCode,  HashMap<String, LinkedList<String>> listOutput, HashMap<String, String> mapStopPoints) {
		
		String stopPointId = mapStopPoints.get(shapeSequence);
		
		if (stopPointId == null) {
			stopPointId = "-";
		} else {
			
		}

		String problem;

		try {
			problem = Problem.getById(Integer.valueOf(problemCode));
		} catch (Exception e) {
			problem = "BETWEEN";
		}

		String outputString = route + SEPARATOR + tripNum + SEPARATOR + shapeId
				+ SEPARATOR + shapeSequence + SEPARATOR + shapeLat + SEPARATOR + shapeLon
				+ SEPARATOR + distanceTraveledShape + SEPARATOR + busCode + SEPARATOR
				+ gpsPointId + SEPARATOR + gpsLat + SEPARATOR + gpsLon + SEPARATOR
				+ distanceToShapePoint + SEPARATOR + timestamp + SEPARATOR + stopPointId
				+ SEPARATOR + problem ;

		if (!listOutput.containsKey(busCode)) {
			listOutput.put(busCode, new LinkedList<String>());
		}
		listOutput.get(busCode).add(outputString);
	}

	public static void generateOutputFromPointsInBetween(String shapeId, String tripNum,
			Tuple2<Float, String> previousGPSPoint, LinkedList<Integer> pointsBetweenGPS,
			Tuple2<Float, String> nextGPSPoint, LinkedList<ShapePoint> listGeoPointsShape, String busCode,
			HashMap<String, LinkedList<String>> listOutput, HashMap<String, String> mapStopPoints) throws ParseException {

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
		String latShape;
		String lonShape;
		String route;
		for (Integer indexPointsInBetween : pointsBetweenGPS) {

			currentDistanceTraveled = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled()
					- previousDistanceTraveled;
			generatedTimeDifference = (long) ((currentDistanceTraveled * time) / distanceTraveled);
			generatedTime = previousTime + generatedTimeDifference;
			generatedTimeString = getTimeString(generatedTime);
			sequence = listGeoPointsShape.get(indexPointsInBetween).getPointSequence();
			latShape = listGeoPointsShape.get(indexPointsInBetween).getLatitude();
			lonShape = listGeoPointsShape.get(indexPointsInBetween).getLongitude();
			route = listGeoPointsShape.get(indexPointsInBetween).getRoute();
			distance = listGeoPointsShape.get(indexPointsInBetween).getDistanceTraveled().toString();

			addOutput(route, tripNum, shapeId, sequence, latShape, lonShape, distance, busCode, "-",
					"-", "-", "-", generatedTimeString, "-", listOutput, mapStopPoints);
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
	
	public static int getNumberTicketsOfBusStop(String currentBusCode, String currentTimeString, String nextTimeString, HashMap<String, LinkedList<TicketInformation>> tickets) throws ParseException {
		
		 SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
		 Date currentTime = sdf.parse(currentTimeString);
	     Date nextTime = sdf.parse(nextTimeString);
	     int count = 0;
	     LinkedList<TicketInformation> times = tickets.get(currentBusCode);
	     if (times != null) {
			for (TicketInformation timeinfo : times) {
				String time = timeinfo.getTimeOfUse();
				 Date date = sdf.parse(time);
				 if (date.after(currentTime) && (date.before(nextTime) || date.equals(nextTime))) {
					count++;
				 }
			}
	     }
		
		return count;
		
	}
}
