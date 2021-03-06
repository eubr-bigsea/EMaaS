package PointMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.Accumulator;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.KeyValueGroupedDataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;

import BULMADependences.GeoLine;
import BULMADependences.GeoObject;
import PointDependencies.FieldsInputsMatchUp;
import PointDependencies.GeoPoint2;
import PointDependencies.PointPair;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import scala.Tuple2;


/**
 * Context-based matching of bus stops (streets and bus stops)
 * 
 * @author Brasileiro
 *
 */
public final class ContextMatchingBusStops20 {

	private static final String FILE_SEPARATOR = ";";
	private static final int NUMBER_ATTRIBUTES_INPUTS = 4;
	
	
	
	/**
	 * Generates DataFrames from csv input files
	 */
	public static Dataset<GeoObject> generateDataFrames(String dataset1CSV, String dataset2CSV, String dataset3CSV, String paramsDataset1, String paramsDataset2,String paramsDataset3, SparkSession spark ) throws Exception {

		Dataset<Row> dataset1 = spark.read().text(dataset1CSV);
		Dataset<Row> dataset2 = spark.read().text(dataset2CSV);
		Dataset<Row> dataset3 = spark.read().text(dataset3CSV);
		
		FieldsInputsMatchUp fieldsDataset1  = new FieldsInputsMatchUp(paramsDataset1);
		FieldsInputsMatchUp fieldsDataset2  = new FieldsInputsMatchUp(paramsDataset2);
		FieldsInputsMatchUp fieldsDataset3  = new FieldsInputsMatchUp(paramsDataset3);
		
		return generateDataFrames(dataset1, dataset2, dataset3, fieldsDataset1, fieldsDataset2, fieldsDataset3, spark);
		
	}
	
	public static Dataset<GeoObject> generateDataFrames(Dataset<Row> dataSourceDS1, Dataset<Row> dataSourceDS2, Dataset<Row> dataSourceContext, FieldsInputsMatchUp fieldsDataset1, FieldsInputsMatchUp fieldsDataset2, FieldsInputsMatchUp fieldsDataset3, SparkSession spark) throws Exception {
		
		final int[] arrayIndexFieldsInputDS1 = new int[NUMBER_ATTRIBUTES_INPUTS];
		final int[] arrayIndexFieldsInputDS2 = new int[NUMBER_ATTRIBUTES_INPUTS];
		final int[] arrayIndexFieldsInputDS3 = new int[NUMBER_ATTRIBUTES_INPUTS];
		
		dataSourceDS1 = removeHeaderDS(dataSourceDS1, fieldsDataset1, arrayIndexFieldsInputDS1);
		dataSourceDS2 = removeHeaderDS(dataSourceDS2, fieldsDataset2, arrayIndexFieldsInputDS2);
		dataSourceContext = removeHeaderDS(dataSourceContext, fieldsDataset3, arrayIndexFieldsInputDS3);
		
		JavaRDD<Row> rddRowDS1 = dataSourceDS1.toJavaRDD();		
		JavaRDD<Row> rddRowDS2 = dataSourceDS2.javaRDD();
		JavaRDD<Row> rddRowDSContext = dataSourceContext.javaRDD();
		
		JavaRDD<GeoObject> rddGeoPointsDS1 =  rddRowDS1.map(new Function<Row, GeoObject>() {

			public GeoObject call(Row s) throws Exception {
				return createGeoPoint(s.getString(0), InputTypes.GOV_POLYGON, arrayIndexFieldsInputDS1);
			}
			
		});
		
		JavaRDD<GeoObject> rddGeoPointsDS2 =  rddRowDS2.map(new Function<Row, GeoObject>() {

			public GeoObject call(Row s) throws Exception {
				return createGeoPoint(s.getString(0), InputTypes.OSM_POLYGON, arrayIndexFieldsInputDS2);
			}

		});
		
		JavaRDD<GeoObject> rddGeoLinesDSContext = rddRowDSContext.map(new Function<Row, GeoObject>() {

			public GeoObject call(Row r) throws Exception {
				return createGeoLine(r.getString(0), InputTypes.GOV_POLYGON, arrayIndexFieldsInputDS3);
			}
			
		});
		
		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Dataset<GeoObject> pointsDS1 = spark.createDataset(JavaRDD.toRDD(rddGeoPointsDS1), geoObjEncoder);
		Dataset<GeoObject> pointsDS2 = spark.createDataset(JavaRDD.toRDD(rddGeoPointsDS2), geoObjEncoder);
		Dataset<GeoObject> contextDS = spark.createDataset(JavaRDD.toRDD(rddGeoLinesDSContext), geoObjEncoder);
		
		Dataset<GeoObject> points = pointsDS1.union(pointsDS2).union(contextDS);
		
		return points;
	}
	
	private static Dataset<Row> removeHeaderDS (Dataset<Row> dataset, final FieldsInputsMatchUp fieldsInputDS, final int[] arraySequence) {
		final Row header = dataset.first();
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			public boolean call(Row value) throws Exception {
				if (value.equals(header)) {

					String[] fields = value.getString(0).split(FILE_SEPARATOR);

					for (int i = 0; i < NUMBER_ATTRIBUTES_INPUTS; i++) {

						if (fields[i].equals(fieldsInputDS.getGeometry())) {
							arraySequence[0] = i;
						} else if (fields[i].equals(fieldsInputDS.getName())) {
							arraySequence[1] = i;
						} else if (fields[i].equals(fieldsInputDS.getIndexOfID())) {
							arraySequence[2] = i;
						} else if (fields[i].equals(fieldsInputDS.getId())) {
							arraySequence[3] = i;
						} else {
							throw new Exception("Input fields do not match Input file fields.");
						}
					}
					return false;
				}
				
				if (value.getString(0).split(FILE_SEPARATOR)[0].contains("INF")) {
					return false;
				}
				
				return true;
			}
		});

		return dataset;
	}

	public static Dataset<String> run(Dataset<GeoObject> points, double thresholdLinguistic, final double thresholdPointDistance, Integer amountPartition, SparkSession spark) throws Exception {
		JavaSparkContext ctx = new JavaSparkContext(spark.sparkContext());
		final Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Encoder<Tuple2<Integer, GeoObject>> tupleEncoder = Encoders.tuple(Encoders.INT(), geoObjEncoder);
		
		Dataset<Tuple2<Integer, GeoObject>> pointsPaired = points.flatMap(new FlatMapFunction<GeoObject, Tuple2<Integer, GeoObject>>() {

			public Iterator<Tuple2<Integer, GeoObject>> call(GeoObject s) throws Exception {
				List<Tuple2<Integer, GeoObject>> listOfPointTuple = new ArrayList<Tuple2<Integer, GeoObject>>();
				if (s.getType().equals(InputTypes.OSM_POLYGON)) {
					listOfPointTuple.add(new Tuple2<Integer, GeoObject>(s.getIdGeometry()%numReplication.getValue(), s));
					return listOfPointTuple.iterator();
				} else { //equals to InputTypes.GOV_POLYGON
					for (int i = 0; i < numReplication.value(); i++) {
						listOfPointTuple.add(new Tuple2<Integer, GeoObject>(i, s));
					}
					return listOfPointTuple.iterator();
				}
			}
			
		}, tupleEncoder);
		
		
		KeyValueGroupedDataset<Integer, Tuple2<Integer, GeoObject>> pointsGrouped = pointsPaired.groupByKey(new MapFunction<Tuple2<Integer, GeoObject>, Integer>() {

			public Integer call(Tuple2<Integer, GeoObject> value) throws Exception {
				return value._1();
			}
		}, Encoders.INT());

		Accumulator<Double> accum = ctx.accumulator(0.0);
		
		Encoder<PointPair> pairEncoder = Encoders.javaSerialization(PointPair.class);
		Encoder<Tuple2<Integer, PointPair>> tuplePairEncoder = Encoders.tuple(Encoders.INT(), pairEncoder);

		Dataset<Tuple2<Integer, PointPair>> matches = pointsGrouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoObject>, Tuple2<Integer, PointPair>>() {
			public Iterator<Tuple2<Integer, PointPair>> call(Integer key, Iterator<Tuple2<Integer, GeoObject>> values)
					throws Exception {
						List<Tuple2<Integer, GeoObject>> pointsPerKey = IteratorUtils.toList(values);
						List<GeoPoint2> pointsSource = new ArrayList<GeoPoint2>();
						List<GeoPoint2> pointsTarget = new ArrayList<GeoPoint2>();
						List<GeoLine> context = new ArrayList<GeoLine>();
						for (Tuple2<Integer, GeoObject> entityPair : pointsPerKey) {
							if (entityPair._2().getType() == InputTypes.OSM_POLYGON) {
								pointsSource.add((GeoPoint2) entityPair._2());
							} else {
								if (entityPair._2() instanceof GeoPoint2) {
									pointsTarget.add((GeoPoint2) entityPair._2());
								} else {
									context.add((GeoLine) entityPair._2());
								}

							}
						}

						List<Tuple2<Integer, PointPair>> entityMatches = new ArrayList<Tuple2<Integer, PointPair>>();

						for (GeoPoint2 entSource : pointsSource) {
							List<Tuple2<Integer, PointPair>> partialResults = new ArrayList<Tuple2<Integer, PointPair>>();
							PriorityQueue<GeoPoint2> neighborhoodBustops = new PriorityQueue<GeoPoint2>();
							List<GeoLine> neighborhoodContext = new ArrayList<GeoLine>();
							GeometryFactory gf = new GeometryFactory();

							for (int i = 0; i < Math.max(pointsTarget.size(), context.size()); i++) {
								if (i < pointsTarget.size()
										&& entSource.getDistance(pointsTarget.get(i)) <= thresholdPointDistance) {
									pointsTarget.get(i).setDistanceToPOI(entSource.getDistance(pointsTarget.get(i)));
									neighborhoodBustops.add(pointsTarget.get(i));
								}

								if (i < context.size() && context.get(i).getGeometry()
										.distance(entSource.getGeometry()) <= thresholdPointDistance) {
									neighborhoodContext.add(context.get(i));
								}
							}

							for (GeoPoint2 point : neighborhoodBustops) {
								Geometry line = gf.createLineString(new Coordinate[] {
										point.getGeometry().getCoordinate(), entSource.getGeometry().getCoordinate() });
								PointPair pair;
								int intersections = 0;
								for (GeoLine street : neighborhoodContext) {
									if (line.intersects(street.getGeometry())) {
										intersections++;
									}
								}

								if (intersections == 0) { // first case (points
															// are near and in
															// same side of the
															// street)
									pair = new PointPair(entSource, point, 0, point.getDistanceToPOI(),
											PolygonClassification.MATCH);
									int index = partialResults.size();
									partialResults.add(new Tuple2<Integer, PointPair>(index, pair));
								} else {
									pair = new PointPair(entSource, point, 0, point.getDistanceToPOI(),
											PolygonClassification.POSSIBLE_PROBLEM);
									int index = partialResults.size();
									partialResults.add(new Tuple2<Integer, PointPair>(index, pair));
								}

							}

							Tuple2<Integer, PointPair> previous = null;
							for (Tuple2<Integer, PointPair> pointPair : partialResults) {
								if (previous != null) {
									if (previous._2().getPolygonClassification().equals(PolygonClassification.MATCH)
											&& pointPair._2().getPolygonClassification()
													.equals(PolygonClassification.POSSIBLE_PROBLEM)) { // case
																										// 1
										entityMatches.add(previous);
										break;
									} else if (previous._2().getPolygonClassification()
											.equals(PolygonClassification.POSSIBLE_PROBLEM)
											&& pointPair._2().getPolygonClassification()
													.equals(PolygonClassification.MATCH)) { // case
																							// 2
										pointPair._2().setPolygonClassification(PolygonClassification.POSSIBLE_PROBLEM);

										entityMatches.add(previous);
										entityMatches.add(pointPair);
									} else if (previous._2().getPolygonClassification()
											.equals(PolygonClassification.MATCH)
											&& pointPair._2().getPolygonClassification()
													.equals(PolygonClassification.MATCH)) { // case
																							// 3
										double maxDistance = Math.max(previous._2().getTarget().getDistanceToPOI(),
												pointPair._2().getTarget().getDistanceToPOI());

										if (pointPair._2().getTarget().getDistanceToPOI() > (2
												* previous._2().getTarget().getDistanceToPOI())) {
											pointPair._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											entityMatches.add(previous);
										} /*
											 * else if
											 * (previous._2().getTarget().
											 * getDistance(pointPair._2().
											 * getTarget()) < (maxDistance)) {
											 * previous._2().
											 * setPolygonClassification(
											 * PolygonClassification.
											 * POSSIBLE_PROBLEM);
											 * pointPair._2().
											 * setPolygonClassification(
											 * PolygonClassification.
											 * POSSIBLE_PROBLEM);
											 * entityMatches.add(previous);
											 * entityMatches.add(pointPair); }
											 */ else {
											// previous._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											// pointPair._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											previous._2()
													.setPolygonClassification(PolygonClassification.POSSIBLE_PROBLEM);
											pointPair._2()
													.setPolygonClassification(PolygonClassification.POSSIBLE_PROBLEM);
											entityMatches.add(previous);
											entityMatches.add(pointPair);
										}
									} else { // POSSIBLE_PROBLEM &&
												// POSSIBLE_PROBLEM
										if (pointPair._2().getTarget().getDistanceToPOI() > (2
												* previous._2().getTarget().getDistanceToPOI())) {
											pointPair._2().setPolygonClassification(PolygonClassification.NON_MATCH);
											entityMatches.add(previous);
										} else {
											entityMatches.add(previous);
											entityMatches.add(pointPair);
										}
									}

									break;

								} else {
									if (partialResults.size() == 1) {
										entityMatches.add(pointPair);
									} else {
										previous = pointPair;
									}
								}
							}
						}

						return entityMatches.iterator();
					}
				}, tuplePairEncoder).repartition(amountPartition);

		Dataset<String> output = matches.flatMap(new FlatMapFunction<Tuple2<Integer, PointPair>, String>() {

			public Iterator<String> call(Tuple2<Integer, PointPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput.iterator();
			}
		
		}, Encoders.STRING()).repartition(amountPartition);
		
		return output;
	}

	private static GeoPoint2 createGeoPoint(String row, InputTypes inputType, int[] arraySequence) throws ParseException {
		
		String[] splittedRow = row.split(FILE_SEPARATOR);
		
		Integer index = null;
		Integer id = null;
		try {
			index = Integer.valueOf(splittedRow[arraySequence[2]]);
			id = Integer.valueOf(splittedRow[arraySequence[3]]);
		} catch (NumberFormatException e) {
			System.err.println("Index and Id of the Geometry should be integer numbers.");
		}
		
		return new GeoPoint2(splittedRow[arraySequence[0]], splittedRow[arraySequence[1]], inputType, index, id);
	}
	
	private static GeoLine createGeoLine(String row, InputTypes inputType, int[] arraySequence) throws ParseException {
		String[] splittedRow = row.split(FILE_SEPARATOR);
		
		Integer index = null;
		Integer id = null;
		try {
			index = Integer.valueOf(splittedRow[arraySequence[2]]);
			id = Integer.valueOf(splittedRow[arraySequence[3]]);
		} catch (NumberFormatException e) {
			System.err.println("Index and Id of the Geometry should be integer numbers.");
		}
		
		return new GeoLine(splittedRow[arraySequence[0]], splittedRow[arraySequence[1]], inputType, index, id);
	}
	
}
