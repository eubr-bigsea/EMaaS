package PolygonMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
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

import PointDependencies.FieldsInputsMatchUp;
import PolygonDependencies.GeoPolygon;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import PolygonDependencies.PolygonPair;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingGeoPolygon20 {

	private static final String FILE_SEPARATOR = ";";
	private static final int NUMBER_ATTRIBUTES_INPUTS = 4;
	
	/**
	 * Generates DataFrames from csv input files
	 */
	public static Dataset<GeoPolygon> generateDataFrames(String dataset1CSV, String dataset2CSV, String paramsDataset1, String paramsDataset2, SparkSession spark) throws Exception {

		Dataset<Row> dataset1 = spark.read().text(dataset1CSV);
		Dataset<Row> dataset2 = spark.read().text(dataset2CSV);
		
		FieldsInputsMatchUp fieldsDataset1  = new FieldsInputsMatchUp(paramsDataset1);
		FieldsInputsMatchUp fieldsDataset2  = new FieldsInputsMatchUp(paramsDataset2);
		
		return generateDataFrames(dataset1, dataset2, fieldsDataset1, fieldsDataset2, spark);
		
	}
		
	public static Dataset<GeoPolygon> generateDataFrames(Dataset<Row> dataSource1, Dataset<Row> dataSource2, FieldsInputsMatchUp fieldsDataset1, FieldsInputsMatchUp fieldsDataset2, SparkSession spark) throws Exception {
		
		final int[] arrayIndexFieldsInputDS1 = new int[NUMBER_ATTRIBUTES_INPUTS];
		final int[] arrayIndexFieldsInputDS2 = new int[NUMBER_ATTRIBUTES_INPUTS];
		dataSource1 = removeHeader(dataSource1, fieldsDataset1, arrayIndexFieldsInputDS1);
		dataSource2 = removeHeader(dataSource2, fieldsDataset2, arrayIndexFieldsInputDS2);
		
		JavaRDD<Row> rddRowDataSource1 = dataSource1.toJavaRDD();		
		JavaRDD<Row> rddRowDataSource2 = dataSource2.javaRDD();
		
		JavaRDD<GeoPolygon> rddGeoentitiesDS1 =  rddRowDataSource1.map(new Function<Row, GeoPolygon>() {

			public GeoPolygon call(Row s) throws Exception {
				return createGeoPolygon(s.getString(0), InputTypes.GOV_POLYGON, arrayIndexFieldsInputDS1);
			}
			
		});
		
		JavaRDD<GeoPolygon> rddGeoentitiesDS2 =  rddRowDataSource2.map(new Function<Row, GeoPolygon>() {

			public GeoPolygon call(Row s) throws Exception {
				return createGeoPolygon(s.getString(0), InputTypes.OSM_POLYGON, arrayIndexFieldsInputDS2);
			}

		});
		
		Encoder<GeoPolygon> polygonEncoder = Encoders.javaSerialization(GeoPolygon.class);
		
		Dataset<GeoPolygon> polygonsDS1 = spark.createDataset(JavaRDD.toRDD(rddGeoentitiesDS1), polygonEncoder);
		Dataset<GeoPolygon> polygonsDS2 = spark.createDataset(JavaRDD.toRDD(rddGeoentitiesDS2), polygonEncoder);
		
		Dataset<GeoPolygon> polygons = polygonsDS1.union(polygonsDS2);
		
		return polygons;
	}
	
	public static Dataset<String> run(Dataset<GeoPolygon> polygons, final double thresholdLinguistic, final double thresholdPolygon, Integer amountPartition, SparkSession spark) throws Exception {
		JavaSparkContext ctx = new JavaSparkContext(spark.sparkContext());
		final Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		Encoder<GeoPolygon> polygonEncoder = Encoders.javaSerialization(GeoPolygon.class);
		
		Encoder<Tuple2<Integer, GeoPolygon>> tupleEncoder = Encoders.tuple(Encoders.INT(), polygonEncoder);
		
		Dataset<Tuple2<Integer, GeoPolygon>> polygonLabed = polygons.flatMap(new FlatMapFunction<GeoPolygon, Tuple2<Integer, GeoPolygon>>() {

			public Iterator<Tuple2<Integer, GeoPolygon>> call(GeoPolygon s) throws Exception {
				List<Tuple2<Integer, GeoPolygon>> listOfPolygonTuple = new ArrayList<Tuple2<Integer, GeoPolygon>>();
				if (s.getType().equals(InputTypes.OSM_POLYGON)) {
					listOfPolygonTuple.add(new Tuple2<Integer, GeoPolygon>(s.getIdGeometry()%numReplication.getValue(), s));
					return listOfPolygonTuple.iterator();
				} else { //equals to InputTypes.GOV_POLYGON
					for (int i = 0; i < numReplication.value(); i++) {
						listOfPolygonTuple.add(new Tuple2<Integer, GeoPolygon>(i, s));
					}
					return listOfPolygonTuple.iterator();
				}
			}
			
		}, tupleEncoder);
		
		
		KeyValueGroupedDataset<Integer, Tuple2<Integer, GeoPolygon>> polygonsGrouped = polygonLabed.groupByKey(new MapFunction<Tuple2<Integer, GeoPolygon>, Integer>() {

			public Integer call(Tuple2<Integer, GeoPolygon> value) throws Exception {
				return value._1();
			}
		}, Encoders.INT());
		
		
		
		Encoder<PolygonPair> pairEncoder = Encoders.javaSerialization(PolygonPair.class);
		Encoder<Tuple2<Integer, PolygonPair>> tuplePairEncoder = Encoders.tuple(Encoders.INT(), pairEncoder);
		
		Dataset<Tuple2<Integer, PolygonPair>> matches = polygonsGrouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoPolygon>, Tuple2<Integer, PolygonPair>>() {
			
			public Iterator<Tuple2<Integer, PolygonPair>> call(Integer key, Iterator<Tuple2<Integer, GeoPolygon>> values)
					throws Exception {
				List<Tuple2<Integer, GeoPolygon>> polygonsPerKey = IteratorUtils.toList(values);
				List<GeoPolygon> polygonsSource = new ArrayList<GeoPolygon>();
				List<GeoPolygon> polygonsTarget = new ArrayList<GeoPolygon>();
				for (Tuple2<Integer, GeoPolygon> entity : polygonsPerKey) {
					if (entity._2().getType() == InputTypes.OSM_POLYGON) {
						polygonsSource.add(entity._2());
					} else {
						polygonsTarget.add(entity._2());
					}
				}
				
				List<Tuple2<Integer, PolygonPair>> entityMatches = new ArrayList<Tuple2<Integer, PolygonPair>>();
				JaccardSimilarity jaccard = new JaccardSimilarity();
				for (GeoPolygon entSource : polygonsSource) {
					for (GeoPolygon entTarget : polygonsTarget) {
						double linguisticSimilarity = 0.0;
						//calculate the linguistic similarity
						if (!entTarget.getGeoName().isEmpty()) {
							linguisticSimilarity = jaccard.getSimilarity(entTarget.getGeoName().toLowerCase(), entSource.getGeoName().toLowerCase());
						}
						
						//calculate the polygon similarity
						double polygonSimilarity = entSource.getPolygonSimilarity(entTarget);
						
						//classification of pairs
						PolygonPair pair;
						if (linguisticSimilarity > thresholdLinguistic && polygonSimilarity > thresholdPolygon) {
							pair = new PolygonPair(entSource, entTarget, linguisticSimilarity, polygonSimilarity, PolygonClassification.MATCH);
						} else if (linguisticSimilarity < thresholdLinguistic && polygonSimilarity < thresholdPolygon) {
							pair = new PolygonPair(entSource, entTarget, linguisticSimilarity, polygonSimilarity, PolygonClassification.NON_MATCH);
						} else {
							pair = new PolygonPair(entSource, entTarget, linguisticSimilarity, polygonSimilarity, PolygonClassification.POSSIBLE_PROBLEM);
						}
						
//						int index = entityMatches.size();
//						entityMatches.add(new Tuple2<Integer, PolygonPair>(index, pair));
						
						//for use case 04
						if (pair.getPolygonClassification().equals(PolygonClassification.POSSIBLE_PROBLEM) || pair.getPolygonClassification().equals(PolygonClassification.MATCH)) {
							int index = entityMatches.size();
							entityMatches.add(new Tuple2<Integer, PolygonPair>(index, pair));
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
				}
				return entityMatches.iterator();
			}
			
		}, tuplePairEncoder).repartition(amountPartition);
		
//		Dataset<Tuple2<Integer, GeoPolygon>> code = grouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoPolygon>, Tuple2<Integer, GeoPolygon>>() {
//
//			@Override
//			public Iterator<Tuple2<Integer, GeoPolygon>> call(Integer key, Iterator<Tuple2<Integer, GeoPolygon>> values)
//					throws Exception {
//				@SuppressWarnings("unchecked")
//				List<Tuple2<Integer, GeoPolygon>> ls = IteratorUtils.toList(values);
//				for (Tuple2<Integer, GeoPolygon> person3 : ls) {
//					System.out.println(person3._1 + ": " + person3._2.getGeoName());
//				}
//				return ls.iterator();
//			}
//		}, tupleEncoder);
//		
//		code.show();
		
		Dataset<String> output = matches.flatMap(new FlatMapFunction<Tuple2<Integer, PolygonPair>, String>() {

			public Iterator<String> call(Tuple2<Integer, PolygonPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput.iterator();
			}
		
		}, Encoders.STRING()).repartition(amountPartition);
		
		
//		ctx.stop();
//		ctx.close();
		
//		output.show();
		return output;
	}
	
	private static GeoPolygon createGeoPolygon(String row, InputTypes inputType, int[] arraySequence) throws ParseException {
		String[] splittedRow = row.split(FILE_SEPARATOR);
		
		Integer index = null;
		Integer id = null;
		try {
			index = Integer.valueOf(splittedRow[arraySequence[2]]);
			id = Integer.valueOf(splittedRow[arraySequence[3]]);
		} catch (NumberFormatException e) {
			System.err.println("Index and Id of the Geometry should be integer numbers.");
		}
		
		return new GeoPolygon(splittedRow[arraySequence[0]], splittedRow[arraySequence[1]], inputType, index, id);
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