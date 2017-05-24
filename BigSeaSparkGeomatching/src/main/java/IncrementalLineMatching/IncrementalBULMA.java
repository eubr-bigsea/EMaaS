package IncrementalLineMatching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

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

	// MAP<ROUTE, MAP<SHAPE_ID, SHAPE>>
	private static Map<String, Map<String, ShapeLine>> mapRouteShapeLine = new HashMap<>();
	// MAP<BUS_CODE+ROUTE, TRIP>
	private static Map<String, Trip> mapBusCodeCurrentTrip = new HashMap<>();	
	// MAP<BUS_CODE+ROUTE, LIST<ShapeLine>> true shapes for the bus. Separate cases with rotative shapes.
	private static Map<String, List<ShapeLine>> mapTrueShapes = new HashMap<>();
	// MAP<BUS_CODE+ROUTE+SHAPE_ID, Tuple2<SMALLER_DISTANCE_TO_FIRST_SHAPE_POINT, LIST<GPS_POINT>>
	private static Map<String,Tuple2<Float, List<GPSPoint>>> mapPossibleOutliersPoints =  new HashMap<>();
	// MAP<BUS_CODE+ROUTE+SHAPE_ID, Tuple2<SMALLER_DISTANCE_TO_LAST_SHAPE_POINT, LAST_GPSPOINT>
	private static Map<String, Tuple2<Float, GPSPoint>> mapPossibleEndPoint = new HashMap<>();
	// MAP<BUS_CODE+ROUTE, LIST<TRIP>>
	private static Map<String, List<Trip>> mapBusCodePreviousTrips = new HashMap<>();
	
	private static final double PERCENTAGE_DISTANCE = 0.09;

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 6) {
			System.err.println("Usage: <shape file path> <GPS files hostname> <GPS files port> <output path> "
					+ "<partitions number> <batch duration");
			System.exit(1);
		}

		String pathFileShapes = args[0];
		String hostnameGPS = args[1];
		Integer portGPS = Integer.valueOf(args[2]);
		String pathOutput = args[3];
		int minPartitions = Integer.valueOf(args[4]);
		int batchDuration = Integer.valueOf(args[5]);

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
						Float distanceTraveled = lastPoint.getDistanceTraveled();
						String route = lastPoint.getRoute();
						ShapeLine shapeLine = new ShapeLine(pair._1, lineString, distanceTraveled, lineBlockingKey,
								listGeoPoint, route, greaterDistance);

						int thresholdDistanceCurrentShape = (int) (shapeLine.getDistanceTraveled()
								/ (shapeLine.getListGeoPoints().size() * PERCENTAGE_DISTANCE));
						shapeLine.setThresholdDistance(thresholdDistanceCurrentShape);
						
						if (!mapRouteShapeLine.containsKey(route)) {
							mapRouteShapeLine.put(route, new HashMap<>());
						}
						
						mapRouteShapeLine.get(route).put(pair._1, shapeLine);

						return new Tuple2<String, GeoLine>(lineBlockingKey, shapeLine);
					}
		});

		JavaDStream<String> gpsStream = context.socketTextStream(hostnameGPS, portGPS);

		JavaDStream<GPSPoint> gpsPointsRDD = gpsStream.map(new Function<String, GPSPoint>() {

				@Override
				public GPSPoint call(String entry) throws Exception {
					return GPSPoint.createGPSPointWithId(entry);
				}
		});
		
		JavaDStream<Tuple3<GPSPoint, ShapePoint, Float>> similarityOutput = gpsPointsRDD.flatMap(new FlatMapFunction<GPSPoint, Tuple3<GPSPoint, ShapePoint, Float>>() {

			@Override
			public Iterator<Tuple3<GPSPoint, ShapePoint, Float>> call(GPSPoint currentGPSPoint) throws Exception {
				List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput = new ArrayList<>();
				String route = currentGPSPoint.getLineCode();
				
//				if (route.equals("207") && currentGPSPoint.getBusCode().equals("BC301")) {
//				if (route.equals("022") && currentGPSPoint.getBusCode().equals("GL323")) {
				if (route.equals("030") && currentGPSPoint.getBusCode().equals("KB603")) {
					populateTrueShapes(route, currentGPSPoint);
				
					Trip trip = mapBusCodeCurrentTrip.get(currentGPSPoint.getBusCode() + route);
					if (trip == null) {
						trip = new Trip();
					}					
						
					if (trip.hasFoundInitialPoint()) {
						
						ShapeLine shapeMatched = trip.getShapeMatched();
						
						ShapePoint startPointShape = (ShapePoint) shapeMatched.getFirstPoint();
						ShapePoint endPointShape = (ShapePoint) shapeMatched.getLastPoint();
						float currentDistanceToStartPoint = GeoPoint.getDistanceInMeters(currentGPSPoint,
								startPointShape);
						float currentDistanceToEndPoint = GeoPoint.getDistanceInMeters(currentGPSPoint,
								endPointShape);
						
						int threshold = shapeMatched.getThresholdDistance();
						
						// TODO What to do if the last point is missing?
						if (currentDistanceToStartPoint > threshold && currentDistanceToEndPoint <= threshold) {
							if (mapPossibleEndPoint.get(currentGPSPoint.getBusCode() + route + shapeMatched.getId()) != null) {
								if (mapPossibleEndPoint.get(currentGPSPoint.getBusCode() + route + shapeMatched.getId())._1 <= currentDistanceToEndPoint) {
									mapPossibleEndPoint.put(currentGPSPoint.getBusCode() + route + shapeMatched.getId(), new Tuple2<Float, GPSPoint>(currentDistanceToEndPoint, currentGPSPoint));
								
								} else { // found last point
									trip.setEndPoint(mapPossibleEndPoint.get(currentGPSPoint.getBusCode() + route + shapeMatched.getId())._2);
									cleanMaps(currentGPSPoint, shapeMatched, route, trip);
									trip = new Trip();
									trip.setInitialPoint(currentGPSPoint);
								}
								
							} else {
								mapPossibleEndPoint.put(currentGPSPoint.getBusCode() + route + shapeMatched.getId(), new Tuple2<Float, GPSPoint>(currentDistanceToEndPoint, currentGPSPoint));
							}	
						} 
						
						addClosestPoint(currentGPSPoint, shapeMatched, trip, listOutput);
						mapBusCodeCurrentTrip.put(currentGPSPoint.getBusCode() + route, trip);
						
					} else { // searching the first point
						
						if (mapTrueShapes.get(currentGPSPoint.getBusCode() + route) != null) {

							for (ShapeLine shapeLine : mapTrueShapes.get(currentGPSPoint.getBusCode() + route)) {
								
								Tuple2<Float, List<GPSPoint>> possibleOutliersPointsAndDistance = mapPossibleOutliersPoints.get(currentGPSPoint.getBusCode() + route + shapeLine.getId());

								List<GPSPoint> listPossibleOutiliers;
								Float smallerDistanceToFirstPoint = Float.MAX_VALUE;
								if (possibleOutliersPointsAndDistance != null) {
									smallerDistanceToFirstPoint = possibleOutliersPointsAndDistance._1;
									listPossibleOutiliers = possibleOutliersPointsAndDistance._2;
								} else {
									 listPossibleOutiliers = new ArrayList<>();
								}
								
								ShapePoint startPointShape = (ShapePoint) shapeLine.getFirstPoint();
								float currentDistanceToFirstPoint = GeoPoint.getDistanceInMeters(currentGPSPoint, startPointShape);
								
								if (currentDistanceToFirstPoint < smallerDistanceToFirstPoint
										|| smallerDistanceToFirstPoint > shapeLine.getGreaterDistancePoints()) {
									
									smallerDistanceToFirstPoint = currentDistanceToFirstPoint;
									listPossibleOutiliers.add(currentGPSPoint);
																		
									mapPossibleOutliersPoints.put(currentGPSPoint.getBusCode() + route + shapeLine.getId(),
											new Tuple2<Float, List<GPSPoint>>(smallerDistanceToFirstPoint,
													listPossibleOutiliers));
									
								} else if (smallerDistanceToFirstPoint <= shapeLine.getGreaterDistancePoints()) { // found initial point
									// TODO Check threshold: errado para pontos iniciais
									// TODO Check which shape it chose: errado para circulares
									
									for (int i = 0; i < listPossibleOutiliers.size() - 1; i++) {
										GPSPoint currentProblemanticPoint = listPossibleOutiliers.get(i);
										Tuple3<ShapeLine, ShapePoint, Float> firstProblematicPoint = getFirstProblematicPoint(currentProblemanticPoint, route);
										
										if (!trip.hasFoundInitialPoint()) {
											if (firstProblematicPoint == null) {
												trip.addOutlierBefore(currentProblemanticPoint);
												listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentProblemanticPoint, null, null));
												
											} else {
												trip.setInitialPoint(currentProblemanticPoint);
												trip.setHasFoundInitialPoint(true);
												trip.addPointToPath(currentProblemanticPoint, firstProblematicPoint._2(), firstProblematicPoint._3());
												trip.setShapeMatched(firstProblematicPoint._1());
												listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentProblemanticPoint, firstProblematicPoint._2(), firstProblematicPoint._3()));
											}
											
										} else {
											addClosestPoint(currentProblemanticPoint, trip.getShapeMatched(), trip, listOutput);
										}										
									}
									
									mapBusCodeCurrentTrip.put(currentGPSPoint.getBusCode() + route, trip);
									cleanMaps(currentGPSPoint, shapeLine, route, trip);
									
									trip = new Trip();
//									trip.setInitialPoint(currentGPSPoint);									
									trip.setHasFoundInitialPoint(true);
									GPSPoint initialGPSPoint = listPossibleOutiliers.get(listPossibleOutiliers.size() - 1);
									trip.setInitialPoint(initialGPSPoint);
									trip.setShapeMatched(shapeLine);

									addClosestPoint(initialGPSPoint, shapeLine, trip, listOutput);
									addClosestPoint(currentGPSPoint, shapeLine, trip, listOutput);
									trip.addPointToPath(initialGPSPoint, startPointShape,
											smallerDistanceToFirstPoint);
									trip.addPointToPath(currentGPSPoint, startPointShape,
											smallerDistanceToFirstPoint);
									mapBusCodeCurrentTrip.put(currentGPSPoint.getBusCode() + route, trip);
									break;
									
								} 
							}
							
						} else { // when doesn't have shape in the dataset
							listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(currentGPSPoint, null, Float.valueOf(Problem.NO_SHAPE.getCode())));
						}
					}
				}
				return listOutput.iterator();
			}
			
			private Tuple3<ShapeLine, ShapePoint, Float> getFirstProblematicPoint(GPSPoint gpsPoint, String route) {

				Tuple3<ShapeLine, ShapePoint, Float> tupleSmallerDistanceTraveled = null;
				Float currentDistanceTraveled;
				for (ShapeLine  shapeLine : mapTrueShapes.get(gpsPoint.getBusCode() + route)) {
				
					Tuple2<ShapePoint, Float> closestPointAndDistance = getClosestShapePoint(shapeLine, gpsPoint);
					
					if (closestPointAndDistance != null) {
						currentDistanceTraveled = closestPointAndDistance._1.getDistanceTraveled();
						
						if (tupleSmallerDistanceTraveled == null || currentDistanceTraveled < tupleSmallerDistanceTraveled._2().getDistanceTraveled()) {
							tupleSmallerDistanceTraveled = new Tuple3<ShapeLine, ShapePoint, Float>(shapeLine, closestPointAndDistance._1, closestPointAndDistance._2);
						}
					}
				}
				
				if (tupleSmallerDistanceTraveled != null && tupleSmallerDistanceTraveled._3() <= tupleSmallerDistanceTraveled._1().getThresholdDistance()) {
					return tupleSmallerDistanceTraveled;
				}
				
				return null;
			}
			

			private void cleanMaps(GPSPoint gpsPoint, ShapeLine shapeMatched, String route, Trip trip) {
				mapPossibleEndPoint.put(gpsPoint.getBusCode() + route + shapeMatched.getId(), null);
				mapBusCodeCurrentTrip.put(gpsPoint.getBusCode() + route, null);
				
				for (ShapeLine shape: mapTrueShapes.get(gpsPoint.getBusCode() + route)) {
	                   mapPossibleOutliersPoints.put(gpsPoint.getBusCode() + route + shape.getId(), null);
	            }
				
				List<Trip> listPreviousTrip = mapBusCodePreviousTrips.get(gpsPoint.getBusCode() + route);
				if (listPreviousTrip == null) {						
					listPreviousTrip = new ArrayList<>();
				}
				listPreviousTrip.add(trip);
				mapBusCodePreviousTrips.put(gpsPoint.getBusCode() + route, listPreviousTrip);
			}

			private void populateTrueShapes(String route, GPSPoint possibleFirstPoint) {
				
				if (mapTrueShapes.get(possibleFirstPoint.getBusCode() + route) == null) {

					List<ShapeLine> listMatchedShapes = new ArrayList<>();				
					Map<String, ShapeLine> mapPossibleShapeLines = mapRouteShapeLine.get(route);
					
					if (mapPossibleShapeLines != null && mapPossibleShapeLines.size() >= 2) {

						ShapeLine bestShape = null;
						Float smallerDistanceTraveled = Float.MAX_VALUE;
						float sumGreaterDistancePoints = 0;
						
						for (Entry<String, ShapeLine> entry : mapPossibleShapeLines.entrySet()) {
							ShapeLine shapeLine = entry.getValue();
							sumGreaterDistancePoints += shapeLine.getGreaterDistancePoints();
							ShapePoint closestPoint = getClosestShapePoint(shapeLine, possibleFirstPoint)._1;
							
							if (closestPoint != null && closestPoint.getDistanceTraveled() <= smallerDistanceTraveled) {
								smallerDistanceTraveled = closestPoint.getDistanceTraveled();
								bestShape = shapeLine;
							}
						}				

						float thresholdShapesSequence = sumGreaterDistancePoints / mapPossibleShapeLines.size();
						
						Point firstPoint = bestShape.getLine().getStartPoint();					
						Point endPoint = bestShape.getLine().getEndPoint();

						if (GPSPoint.getDistanceInMeters(firstPoint, endPoint) <= thresholdShapesSequence) {						
							listMatchedShapes.add(bestShape); // shape circular, apenas 1				
						} else {
							listMatchedShapes.addAll(mapPossibleShapeLines.values()); // shapes complementares
						}
						
					} else if (mapPossibleShapeLines != null && mapPossibleShapeLines.size() == 1) {
						listMatchedShapes.addAll(mapPossibleShapeLines.values());
					}
						
					mapTrueShapes.put(possibleFirstPoint.getBusCode() + route, listMatchedShapes);
				}				
			}

			private Tuple2<ShapePoint, Float> getClosestShapePoint(ShapeLine shapeLine, GPSPoint gpsPoint) {
				Float smallerDistance = Float.MAX_VALUE;
				ShapePoint closestShapePoint = null;
				Float currentDistance;
				for (GeoPoint shapePoint : shapeLine.getListGeoPoints()) {
					currentDistance = GeoPoint.getDistanceInMeters(gpsPoint, shapePoint);
					if (currentDistance < smallerDistance) {
						smallerDistance = currentDistance;
						closestShapePoint = (ShapePoint)shapePoint;
					}
				}
				return new Tuple2<ShapePoint, Float>(closestShapePoint, smallerDistance);
				
			}

			private void addClosestPoint(GPSPoint gpsPoint, ShapeLine shapeLine, Trip trip,
					List<Tuple3<GPSPoint, ShapePoint, Float>> listOutput) throws Exception {
				
				Float smallerDistance = null;
				Float currentDistance;
				for (GeoPoint shapePoint : shapeLine.getListGeoPoints()) {
					currentDistance = GeoPoint.getDistanceInMeters(gpsPoint, shapePoint);
					if (smallerDistance == null || currentDistance < smallerDistance) {
						smallerDistance = currentDistance;
						gpsPoint.setClosestPoint(shapePoint);
					}
				}
				trip.addPointToPath(gpsPoint, gpsPoint.getClosestPoint(), smallerDistance);
				listOutput.add(new Tuple3<GPSPoint, ShapePoint, Float>(gpsPoint, gpsPoint.getClosestPoint(),
						smallerDistance));
			}
		});

		rddShapeLinePair.saveAsTextFile(pathOutput + "Shape");
		similarityOutput.dstream().saveAsTextFiles(pathOutput, "GPS");

		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}