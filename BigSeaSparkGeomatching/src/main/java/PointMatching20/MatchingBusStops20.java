package PointMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

import com.vividsolutions.jts.io.ParseException;

import BULMADependences.GeoObject;
import PointDependencies.FieldsInputsMatchUp;
import PointDependencies.GeoPoint2;
import PointDependencies.PointPair;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingBusStops20 {
	
	private static final String FILE_SEPARATOR = ";";
	private static final int NUMBER_ATTRIBUTES_INPUTS = 4;
	
	/**
	 * Generates DataFrames from csv input files
	 */
	public static Dataset<GeoObject> generateDataFrames(String dataset1CSV, String dataset2CSV, String paramsDataset1, String paramsDataset2, SparkSession spark) throws Exception {

		Dataset<Row> dataset1 = spark.read().text(dataset1CSV);
		Dataset<Row> dataset2 = spark.read().text(dataset2CSV);
		
		FieldsInputsMatchUp fieldsDataset1  = new FieldsInputsMatchUp(paramsDataset1);
		FieldsInputsMatchUp fieldsDataset2  = new FieldsInputsMatchUp(paramsDataset2);
		
		return generateDataFrames(dataset1, dataset2, fieldsDataset1, fieldsDataset2, spark);
		
	}
	
	public static Dataset<GeoObject> generateDataFrames(Dataset<Row> dataset1, Dataset<Row> dataset2, FieldsInputsMatchUp fieldsDataset1, FieldsInputsMatchUp fieldsDataset2, SparkSession spark) throws Exception {
		
		final int[] arrayIndexFieldsInputDS1 = new int[NUMBER_ATTRIBUTES_INPUTS];
		final int[] arrayIndexFieldsInputDS2 = new int[NUMBER_ATTRIBUTES_INPUTS];
		
		dataset1 = removeHeader(dataset1, fieldsDataset1, arrayIndexFieldsInputDS1);
		dataset2 = removeHeader(dataset2, fieldsDataset2, arrayIndexFieldsInputDS2);
		
		JavaRDD<Row> rddRowDataSourceGeoPref = dataset1.toJavaRDD();		
		JavaRDD<Row> rddRowDataSourceGeoOSM = dataset2.javaRDD();
		
		JavaRDD<GeoObject> rddGeoPointsDS1 =  rddRowDataSourceGeoPref.map(new Function<Row, GeoObject>() {

			public GeoObject call(Row s) throws Exception {
				return createGeoPoint(s.getString(0), InputTypes.GOV_POLYGON, arrayIndexFieldsInputDS1);
			}
			
		});
		
		JavaRDD<GeoObject> rddGeoPointsDS2 =  rddRowDataSourceGeoOSM.map(new Function<Row, GeoObject>() {

			public GeoObject call(Row s) throws Exception {
				return createGeoPoint(s.getString(0), InputTypes.OSM_POLYGON, arrayIndexFieldsInputDS2);
			}

		});

		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Dataset<GeoObject> pointsDS1 = spark.createDataset(JavaRDD.toRDD(rddGeoPointsDS1), geoObjEncoder);
		Dataset<GeoObject> pointsDS2 = spark.createDataset(JavaRDD.toRDD(rddGeoPointsDS2), geoObjEncoder);
		
		Dataset<GeoObject> points = pointsDS1.union(pointsDS2);
		
		return points;
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
		
		final Accumulator<Double> accum = ctx.accumulator(0.0);
		
		Encoder<PointPair> pairEncoder = Encoders.javaSerialization(PointPair.class);
		Encoder<Tuple2<Integer, PointPair>> tuplePairEncoder = Encoders.tuple(Encoders.INT(), pairEncoder);

		Dataset<Tuple2<Integer, PointPair>> matches = pointsGrouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoObject>, Tuple2<Integer, PointPair>>() {
			public Iterator<Tuple2<Integer, PointPair>> call(Integer key, Iterator<Tuple2<Integer, GeoObject>> values)
					throws Exception {
					List<Tuple2<Integer, GeoObject>> pointsPerKey = IteratorUtils.toList(values);
					List<GeoPoint2> pointsSource = new ArrayList<GeoPoint2>();
					List<GeoPoint2> pointsTarget = new ArrayList<GeoPoint2>();
					for (Tuple2<Integer, GeoObject> entityPair : pointsPerKey) {
						if (entityPair._2().getType() == InputTypes.OSM_POLYGON) {
							pointsSource.add((GeoPoint2) entityPair._2());
						} else {
							pointsTarget.add((GeoPoint2) entityPair._2());
						}
					}
					
					List<Tuple2<Integer, PointPair>> entityMatches = new ArrayList<Tuple2<Integer, PointPair>>();
					JaccardSimilarity jaccard = new JaccardSimilarity();
					for (GeoPoint2 entSource : pointsSource) {
						double replicateDuplicate = 0.0;
						for (GeoPoint2 entTarget : pointsTarget) {
							double linguisticSimilarity = 0.0;
	//						//calculate the linguistic similarity A BASE DA PREF N TEM NOME
							if (!entTarget.getGeoName().isEmpty()) {
								linguisticSimilarity = jaccard.getSimilarity(entTarget.getGeoName().toLowerCase(), entSource.getGeoName().toLowerCase());
							}
							
							//calculate the polygon similarity
							double distanceSimilarity = entSource.getDistance(entTarget);
							
							//classification of pairs
							PointPair pair;
							if (distanceSimilarity <= thresholdPointDistance /*&& linguisticSimilarity > thresholdLinguistic && (!entSource.getIdInDataset().equals(entTarget.getIdInDataset()))*/) {
								pair = new PointPair(entSource, entTarget, 0, distanceSimilarity, PolygonClassification.MATCH);
							} else {
								pair = new PointPair(entSource, entTarget, 0, distanceSimilarity, PolygonClassification.NON_MATCH);
							}
	//						if (linguisticSimilarity > thresholdLinguistic && distanceSimilarity < thresholdPointDistance) {
	//							pair = new PointPair(entSource, entTarget, linguisticSimilarity, distanceSimilarity, PolygonClassification.MATCH);
	//						} else if (linguisticSimilarity < thresholdLinguistic && distanceSimilarity < thresholdPointDistance) {
	//							pair = new PointPair(entSource, entTarget, linguisticSimilarity, distanceSimilarity, PolygonClassification.NON_MATCH);
	//						} else {
	//							pair = new PointPair(entSource, entTarget, linguisticSimilarity, distanceSimilarity, PolygonClassification.POSSIBLE_PROBLEM);
	//						}
							
							//for use case 04
							if (pair.getPolygonClassification().equals(PolygonClassification.MATCH) /*&& (!pair.getSource().getIdInDataset().equals(pair.getTarget().getIdInDataset()))*/) {
								int index = entityMatches.size();
								entityMatches.add(new Tuple2<Integer, PointPair>(index, pair));
								replicateDuplicate++;
							}
							
	//							if (Math.abs(entTarget.getArea() - entSource.getArea()) > thresholdArea) {
	//								entityMatches.add(new Tuple2<String, String>(entTarget.getGeoName(), entSource.getGeoName() + ":" + Math.abs(entTarget.getArea() - entSource.getArea())));
	////								System.out.println(entTarget.getGeoName() +  " - " + entSource.getGeoNameame(), _2));
	////								System.out.println(entTarget.getGeoName() +  " - " + ());
	////								System.out.println(entTarget.getGeoName() + " pref: " + String.format("%.2f", entTarget.getArea()));
	////								System.out.println(entSource.getGeoName() + " OSM: " + String.format("%.2f", entSource.getArea()));
	////								System.out.println();
	//							}
						}
						accum.add(replicateDuplicate > 0? replicateDuplicate-1 : replicateDuplicate);
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
	
	private static Dataset<Row> removeHeader (Dataset<Row> dataset, final FieldsInputsMatchUp fieldsInputDS, final int[] arraySequence) {
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
				
				if (value.getString(0).split(";")[0].contains("INF")) {
					return false;
				}
				
				return true;
			}
		});

		return dataset;
	}
}
