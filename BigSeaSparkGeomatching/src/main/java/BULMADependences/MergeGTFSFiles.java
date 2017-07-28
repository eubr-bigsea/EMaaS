package BULMADependences;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;

public class MergeGTFSFiles {

	private static final String FILE_SEPARATOR = ",";
	private static final String HEADER_STOP_TIME_FILE = "arrival_time,departure_time,stop_id,stop_sequence,lat_stop,lng_stop,route_id,shape_id,closest_shape_point";

	private static Map<String, String> mapTripRoute = new HashMap<>();
	private static Map<String, String> mapStopLatLng = new HashMap<>();
	private static Map<String, String> mapShapeRouteId = new HashMap<>();
	private static Map<String, String> mapRouteIdRouteCode = new HashMap<>();
	private static Map<String, ShapeLine> mapShapeLines = new HashMap<>();

	public static void main(String[] args) {
		String stopTimes = args[0]; // trip_id,arrival_time,departure_time,stop_id,stop_sequence
		String trips = args[1]; // route_id
		String stops = args[2]; // lat_stop, lng_stop
		String routes = args[3];
		String shapes = args[4];
		String newFile = args[5];
		
//		Uncomment the lines below to generate Shape File
		readRoutesFile(routes);
		readTripFileGetRoute(trips);
		updateShapeFile(shapes, newFile);
		
//	    Uncomment the lines below to generate stops times file
//		readTripFile(trips);
//		readStopsFile(stops);
//		createShapePoints(shapes);
//		createNewFile(newFile, stopTimes);
		
		System.out.println("Done!");
		
	}
	
	private static void createShapePoints(String shapes) {
		BufferedReader brShapes = null;
		String lineShapes = "";
		try {
			brShapes = new BufferedReader(new FileReader(shapes));

			
			String previousId = null;
			brShapes.readLine();	
			List<GeoPoint> listPoints = new ArrayList<>();
			while ((lineShapes = brShapes.readLine()) != null) {

				String[] data = lineShapes.split(FILE_SEPARATOR);
				String shapeId = data[0];
				String route = mapShapeRouteId.get(shapeId);
				if (route == null) {
					route = "-";
				}
				String lat = data[1];
				String lng = data[2];
				String pointSequence = data[3];
				
				ShapePoint currentShapePoint = new ShapePoint(shapeId, lat, lng, pointSequence, null);
				
				if (previousId != null && !previousId.equals(shapeId)) {
					createNewShapeLine(listPoints);
					listPoints = new ArrayList<>();
				}
				listPoints.add(currentShapePoint);
				previousId = shapeId;
				
			}
			createNewShapeLine(listPoints);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void createNewShapeLine(List<GeoPoint> listGeoPoint) {
		@SuppressWarnings("deprecation")
		GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();
		
		List<Coordinate> coordinates = new ArrayList<>();
		
		GeoPoint currentGeoPoint = null;
		for (int i = 0; i < listGeoPoint.size(); i++) {
			currentGeoPoint = listGeoPoint.get(i);
			
			Double latitude = Double.valueOf(currentGeoPoint.getLatitude());
			Double longitude = Double.valueOf(currentGeoPoint.getLongitude());
			coordinates.add(new Coordinate(latitude, longitude));

		}

		Coordinate[] array = new Coordinate[coordinates.size()];

		LineString lineString = geometryFactory.createLineString(coordinates.toArray(array));
		ShapeLine shapeLine = new ShapeLine(((ShapePoint)currentGeoPoint).getId(), lineString, null, null,
				listGeoPoint, null, 0);
		
		mapShapeLines.put(((ShapePoint)currentGeoPoint).getId(), shapeLine);
		
	}

	/**
	 * Gets the closest shape point of the stop point
	 * @param stopPoint
	 * @param shapeLine the shape line matched 
	 * @return tuple with the closest shape point and the distance between it and the stop point
	 */
	private static ShapePoint getClosestShapePoint(GeoPoint geoPoint, ShapeLine shapeLine) {
	
		Float smallerDistance = Float.MAX_VALUE;
		ShapePoint closestShapePoint = null;
		
		Float currentDistance;
		for (GeoPoint shapePoint : shapeLine.getListGeoPoints()) {
			currentDistance = GeoPoint.getDistanceInMeters(geoPoint, shapePoint);
			if (currentDistance < smallerDistance) {
				smallerDistance = currentDistance;
				closestShapePoint = (ShapePoint) shapePoint;
			}
		}
		
		return closestShapePoint;
	}

	private static void updateShapeFile(String shapes, String newFilePath) {
		BufferedReader brShapes = null;
		String lineShapes = "";
		try {
			brShapes = new BufferedReader(new FileReader(shapes));
			FileWriter output = new FileWriter(newFilePath);
			PrintWriter printWriter = new PrintWriter(output);

			printWriter.println("route_id" + FILE_SEPARATOR + brShapes.readLine());
			
			while ((lineShapes = brShapes.readLine()) != null) {

				String[] data = lineShapes.split(FILE_SEPARATOR);
				String shapeId = data[0];
				String routeId = mapShapeRouteId.get(shapeId);
				String routeCode = null;
				if (routeId == null) {
					routeCode = "-";
				} else {
					routeCode = mapRouteIdRouteCode.get(routeId);
					if (routeCode == null) {
						routeCode = "-";
					}
				}
				
				printWriter.println(routeCode + FILE_SEPARATOR + lineShapes);
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
	
	
	private static void readTripFileGetRoute(String trips) {
		
		
		BufferedReader brTrips = null;
		String lineTrips = "";
		
		try {
			brTrips = new BufferedReader(new FileReader(trips));
			brTrips.readLine();
			while ((lineTrips = brTrips.readLine()) != null) {

				String[] data = lineTrips.split(FILE_SEPARATOR);
				String route = data[0];
				String shapeId = data[7];

				if (!mapShapeRouteId.containsKey(shapeId)) {
					mapShapeRouteId.put(shapeId, route);
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
	
	private static void readRoutesFile(String routes) {
		
		BufferedReader brRoute = null;
		String lineRoute = "";
		
		try {
			brRoute = new BufferedReader(new FileReader(routes));
			brRoute.readLine();
			while ((lineRoute = brRoute.readLine()) != null) {

				String[] data = lineRoute.split(FILE_SEPARATOR);
				String routeId = data[0];
				String routeCode = data[2];
				
				if (!mapRouteIdRouteCode.containsKey(routeId)) {
					mapRouteIdRouteCode.put(routeId, routeCode);
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
	
	private static void readTripFile(String trips) {
		BufferedReader brTrips = null;
		String lineTrips = "";
		
		try {
			brTrips = new BufferedReader(new FileReader(trips));
			while ((lineTrips = brTrips.readLine()) != null) {

				String[] data = lineTrips.split(FILE_SEPARATOR);
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
	
	private static void readStopsFile(String stops) {
		BufferedReader brStops = null;
		String lineStops = "";
		try {
			brStops = new BufferedReader(new FileReader(stops));
			while ((lineStops = brStops.readLine()) != null) {

				String[] data = lineStops.split(FILE_SEPARATOR);
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
	
	private static void createNewFile(String newFilePath, String stopTimes) {
		BufferedReader brStopTimes = null;
		
		String lineStopTime = "";

		try {

			FileWriter output = new FileWriter(newFilePath);
			PrintWriter printWriter = new PrintWriter(output);

			printWriter.println(HEADER_STOP_TIME_FILE);
			
			brStopTimes = new BufferedReader(new FileReader(stopTimes));
			brStopTimes.readLine();
			while ((lineStopTime = brStopTimes.readLine()) != null) {

				String[] data = lineStopTime.split(FILE_SEPARATOR);
				String tripId = data[0];
				String arrivalTime = data[1];
				String departureTime = data[2];
				String stopId = data[3];
				String stopSequence = data[4];
				String routeShapeId = mapTripRoute.get(tripId);
				String latlng = mapStopLatLng.get(stopId);

				String lat = latlng.split(FILE_SEPARATOR)[0];
				String lng = latlng.split(FILE_SEPARATOR)[1];
				String shapeId = routeShapeId.split(FILE_SEPARATOR)[1];
				
				ShapePoint closestPoint = getClosestShapePoint(new ShapePoint(null, lat, lng , null, null), mapShapeLines.get(shapeId));
				
				printWriter.print(arrivalTime + FILE_SEPARATOR);
				printWriter.print(departureTime + FILE_SEPARATOR);
				printWriter.print(stopId + FILE_SEPARATOR);
				printWriter.print(stopSequence + FILE_SEPARATOR);
				printWriter.print(latlng + FILE_SEPARATOR);
				printWriter.print(routeShapeId + FILE_SEPARATOR);
				printWriter.println(closestPoint.getPointSequence());
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
