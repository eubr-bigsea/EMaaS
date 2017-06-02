package PolygonMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

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

import PolygonDependencies.GeoPolygon;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import PolygonDependencies.PolygonPair;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingGeoPolygon20 {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 
	
	
	private static GeoPolygon createGeoPolygon(String row, InputTypes inputType) throws ParseException {
		StringTokenizer st = new StringTokenizer(row, ";");
		String geometry = st.nextToken(); 
		String name = st.nextToken();
		int indexOfPref = Integer.valueOf(st.nextToken());
		int id =	Integer.valueOf(st.nextToken());				
		return new GeoPolygon(geometry, name, inputType, indexOfPref, id);
	}
	
	public static Dataset<GeoPolygon> generateDataFrames(Dataset<Row> dataSourceGeoPref, Dataset<Row> dataSourceGeoOSM, SparkSession spark) throws Exception {
		
		dataSourceGeoPref = removeHeader(dataSourceGeoPref);
		dataSourceGeoOSM = removeHeader(dataSourceGeoOSM);
		
		JavaRDD<Row> rddRowDataSourceGeoPref = dataSourceGeoPref.toJavaRDD();		
		JavaRDD<Row> rddRowDataSourceGeoOSM = dataSourceGeoOSM.javaRDD();
		
		JavaRDD<GeoPolygon> rddGeoentitiesPref =  rddRowDataSourceGeoPref.map(new Function<Row, GeoPolygon>() {

			@Override
			public GeoPolygon call(Row s) throws Exception {
				return createGeoPolygon(s.getString(0), InputTypes.GOV_POLYGON);
			}
			
		});
		
		JavaRDD<GeoPolygon> rddGeoentitiesOSM =  rddRowDataSourceGeoOSM.map(new Function<Row, GeoPolygon>() {

			@Override
			public GeoPolygon call(Row s) throws Exception {
				return createGeoPolygon(s.getString(0), InputTypes.OSM_POLYGON);
			}

		});
		
		Encoder<GeoPolygon> polygonEncoder = Encoders.javaSerialization(GeoPolygon.class);
		
		Dataset<GeoPolygon> polygonsOSM = spark.createDataset(JavaRDD.toRDD(rddGeoentitiesOSM), polygonEncoder);
		Dataset<GeoPolygon> polygonsPref = spark.createDataset(JavaRDD.toRDD(rddGeoentitiesPref), polygonEncoder);
		
		Dataset<GeoPolygon> polygons = polygonsPref.union(polygonsOSM);
		
		return polygons;
	}
	
	private static Dataset<Row> removeHeader(Dataset<Row> dataset) {
		Row headerDataset3 = dataset.first();
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean call(Row value) throws Exception {
				if (value.equals(headerDataset3)) {
					return false;
				}
				return true;
			}
		});
		
		return dataset;
	}
	
	public static Dataset<String> run(Dataset<GeoPolygon> polygons, double thresholdLinguistic, double thresholdPolygon, Integer amountPartition, SparkSession spark) throws Exception {
		JavaSparkContext ctx = new JavaSparkContext(spark.sparkContext());
		Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		Encoder<GeoPolygon> polygonEncoder = Encoders.javaSerialization(GeoPolygon.class);
		
		Encoder<Tuple2<Integer, GeoPolygon>> tupleEncoder = Encoders.tuple(Encoders.INT(), polygonEncoder);
		
		Dataset<Tuple2<Integer, GeoPolygon>> polygonLabed = polygons.flatMap(new FlatMapFunction<GeoPolygon, Tuple2<Integer, GeoPolygon>>() {

			@Override
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

			@Override
			public Integer call(Tuple2<Integer, GeoPolygon> value) throws Exception {
				return value._1();
			}
		}, Encoders.INT());
		
		
		
		Encoder<PolygonPair> pairEncoder = Encoders.javaSerialization(PolygonPair.class);
		Encoder<Tuple2<Integer, PolygonPair>> tuplePairEncoder = Encoders.tuple(Encoders.INT(), pairEncoder);
		
		Dataset<Tuple2<Integer, PolygonPair>> matches = polygonsGrouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoPolygon>, Tuple2<Integer, PolygonPair>>() {
			@Override
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

			@Override
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
	
	
}