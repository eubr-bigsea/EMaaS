package BULMADependences;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MergeGTFSFiles {

	private static final String FILE_SEPARATOR = ",";
	private static String[] headerNewFile = { "arrival_time", "departure_time", "stop_id", "stop_sequence",
			"lat_stop", "lng_stop", "route_id", "shape_id" };

	private static Map<String, String> mapTripRoute = new HashMap<>();
	private static Map<String, String> mapStopLatLng = new HashMap<>();
	private static Map<String, String> mapShapeRoute = new HashMap<>();

	public static void main(String[] args) {
		String stopTimes = args[0]; // trip_id,arrival_time,departure_time,stop_id,stop_sequence
		String trips = args[1]; // route_id
		String stops = args[2]; // lat_stop, lng_stop
		String shapes = args[3];
		String newFile = args[4];

		String cvsSplitBy = ",";
		
//		readTripFileGetRoute(trips, cvsSplitBy);
//		updateShapeFile(shapes, newFile, cvsSplitBy);
		
		readTripFile(trips, cvsSplitBy);
		readStopsFile(stops, cvsSplitBy);
		createNewFile(newFile, stopTimes, cvsSplitBy);
		
		System.out.println("Done!");
		
	}
	
	private static void updateShapeFile(String shapes, String newFilePath, String cvsSplitBy) {
		BufferedReader brShapes = null;
		String lineShapes = "";
		try {
			brShapes = new BufferedReader(new FileReader(shapes));
			FileWriter output = new FileWriter(newFilePath);
			PrintWriter printWriter = new PrintWriter(output);

			printWriter.println("route_id" + FILE_SEPARATOR + brShapes.readLine());
			
			while ((lineShapes = brShapes.readLine()) != null) {

				String[] data = lineShapes.split(cvsSplitBy);
				String shapeId = data[0];
				String route = mapShapeRoute.get(shapeId);
				if (route == null) {
					route = "-";
				}
				
				printWriter.println(route + FILE_SEPARATOR + lineShapes);
			}
			output.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void readTripFileGetRoute(String trips, String cvsSplitBy) {
		BufferedReader brTrips = null;
		String lineTrips = "";
		
		try {
			brTrips = new BufferedReader(new FileReader(trips));
			brTrips.readLine();
			while ((lineTrips = brTrips.readLine()) != null) {

				String[] data = lineTrips.split(cvsSplitBy);
				String route = data[0];
				String shapeId = data[7];

				if (!mapShapeRoute.containsKey(shapeId)) {
					mapShapeRoute.put(shapeId, route);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void readTripFile(String trips, String cvsSplitBy) {
		BufferedReader brTrips = null;
		String lineTrips = "";
		
		try {
			brTrips = new BufferedReader(new FileReader(trips));
			while ((lineTrips = brTrips.readLine()) != null) {

				String[] data = lineTrips.split(cvsSplitBy);
				String route = data[0];
				String tripId = data[2];
				String shapeId = data[7];

				if (!mapTripRoute.containsKey(tripId)) {
					mapTripRoute.put(tripId, route + FILE_SEPARATOR + shapeId);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void readStopsFile(String stops, String cvsSplitBy) {
		BufferedReader brStops = null;
		String lineStops = "";
		try {
			brStops = new BufferedReader(new FileReader(stops));
			while ((lineStops = brStops.readLine()) != null) {

				String[] data = lineStops.split(cvsSplitBy);
				String stopId = data[0];
				String lat = data[4];
				String lng = data[5];
				
				if (!lat.startsWith("-")) {
					lat = data[5];
					lng = data[6];
					if (!lat.startsWith("-")) {
						lat = data[6];
						lng = data[7];
					}
				}

				if (!mapStopLatLng.containsKey(stopId)) {
					mapStopLatLng.put(stopId, lat + FILE_SEPARATOR + lng);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void createNewFile(String newFilePath, String stopTimes, String cvsSplitBy) {
		BufferedReader brStopTimes = null;
		
		String lineStopTime = "";

		try {

			FileWriter output = new FileWriter(newFilePath);
			PrintWriter printWriter = new PrintWriter(output);

			printWriter.print(headerNewFile[0] + FILE_SEPARATOR);
			printWriter.print(headerNewFile[1] + FILE_SEPARATOR);
			printWriter.print(headerNewFile[2] + FILE_SEPARATOR);
			printWriter.print(headerNewFile[3] + FILE_SEPARATOR);
			printWriter.print(headerNewFile[4] + FILE_SEPARATOR);
			printWriter.print(headerNewFile[5] + FILE_SEPARATOR);
			printWriter.print(headerNewFile[6] + FILE_SEPARATOR);
			printWriter.println(headerNewFile[7]);

			brStopTimes = new BufferedReader(new FileReader(stopTimes));
			brStopTimes.readLine();
			while ((lineStopTime = brStopTimes.readLine()) != null) {

				String[] data = lineStopTime.split(cvsSplitBy);
				String tripId = data[0];
				String arrivalTime = data[1];
				String departureTime = data[2];
				String stopId = data[3];
				String stopSequence = data[4];
				String routeShapeId = mapTripRoute.get(tripId);
				String latlng = mapStopLatLng.get(stopId);

				printWriter.print(arrivalTime + FILE_SEPARATOR);
				printWriter.print(departureTime + FILE_SEPARATOR);
				printWriter.print(stopId + FILE_SEPARATOR);
				printWriter.print(stopSequence + FILE_SEPARATOR);
				printWriter.print(latlng + FILE_SEPARATOR);
				printWriter.println(routeShapeId);
			}
			
			output.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (brStopTimes != null) {
				try {
					brStopTimes.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
