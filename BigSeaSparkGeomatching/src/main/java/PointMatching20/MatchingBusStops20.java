package PointMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

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
import PointDependencies.GeoPoint2;
import PointDependencies.PointPair;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingBusStops20 {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 
	
	private static GeoPoint2 createGeoPoint(String row, InputTypes inputType) throws ParseException {
		StringTokenizer st = new StringTokenizer(row, ";");
		String geometry = st.nextToken(); 
		String name = st.nextToken();
		int index = Integer.valueOf(st.nextToken());
		int id =	Integer.valueOf(st.nextToken());	
		return new GeoPoint2(geometry, name, inputType, index, id);
	}
	
	public static Dataset<GeoObject> generateDataFrames(Dataset<Row> dataSourceGeoPref, Dataset<Row> dataSourceGeoOSM, SparkSession spark) throws Exception {
		
		dataSourceGeoPref = removeHeader(dataSourceGeoPref);
		dataSourceGeoOSM = removeHeader(dataSourceGeoOSM);
		
		JavaRDD<Row> rddRowDataSourceGeoPref = dataSourceGeoPref.toJavaRDD();		
		JavaRDD<Row> rddRowDataSourceGeoOSM = dataSourceGeoOSM.javaRDD();
		
		JavaRDD<GeoObject> rddGeoPointsPref =  rddRowDataSourceGeoPref.map(new Function<Row, GeoObject>() {

			@Override
			public GeoObject call(Row s) throws Exception {
				return createGeoPoint(s.getString(0), InputTypes.GOV_POLYGON);
			}
			
		});
		
		JavaRDD<GeoObject> rddGeoPointsOSM =  rddRowDataSourceGeoOSM.map(new Function<Row, GeoObject>() {

			@Override
			public GeoObject call(Row s) throws Exception {
				return createGeoPoint(s.getString(0), InputTypes.OSM_POLYGON);
			}

		});

		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Dataset<GeoObject> pointsDS1 = spark.createDataset(JavaRDD.toRDD(rddGeoPointsPref), geoObjEncoder);
		Dataset<GeoObject> pointsDS2 = spark.createDataset(JavaRDD.toRDD(rddGeoPointsOSM), geoObjEncoder);
		
		Dataset<GeoObject> points = pointsDS1.union(pointsDS2);
		
		return points;
	}
	
	private static Dataset<Row> removeHeader(Dataset<Row> dataset) {
		Row headerDataset3 = dataset.first();
		dataset = dataset.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean call(Row value) throws Exception {
				if (value.equals(headerDataset3) || value.getString(0).split(";")[0].contains("INF")) {
					return false;
				}
				return true;
			}
		});
		
		return dataset;
	}

	public static Dataset<String> run(Dataset<GeoObject> points, double thresholdLinguistic, double thresholdPointDistance, Integer amountPartition, SparkSession spark) throws Exception {
		JavaSparkContext ctx = new JavaSparkContext(spark.sparkContext());
		Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		
		
		Encoder<Tuple2<Integer, GeoObject>> tupleEncoder = Encoders.tuple(Encoders.INT(), geoObjEncoder);
		
		Dataset<Tuple2<Integer, GeoObject>> pointsPaired = points.flatMap(new FlatMapFunction<GeoObject, Tuple2<Integer, GeoObject>>() {

			@Override
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
		
//		JavaPairRDD<Integer, GeoPoint2> pointsPaired = pointLabed.mapToPair(new PairFunction<Tuple2<Integer,GeoPoint2>, Integer, GeoPoint2>() {
//
//			@Override
//			public Tuple2<Integer, GeoPoint2> call(Tuple2<Integer, GeoPoint2> tuple) throws Exception {
//				return new Tuple2<Integer, GeoPoint2>(tuple._1(), tuple._2());
//			}
//		});
		
		KeyValueGroupedDataset<Integer, Tuple2<Integer, GeoObject>> pointsGrouped = pointsPaired.groupByKey(new MapFunction<Tuple2<Integer, GeoObject>, Integer>() {

			@Override
			public Integer call(Tuple2<Integer, GeoObject> value) throws Exception {
				return value._1();
			}
		}, Encoders.INT());
		
		Accumulator<Double> accum = ctx.accumulator(0.0);
		
		Encoder<PointPair> pairEncoder = Encoders.javaSerialization(PointPair.class);
		Encoder<Tuple2<Integer, PointPair>> tuplePairEncoder = Encoders.tuple(Encoders.INT(), pairEncoder);

		Dataset<Tuple2<Integer, PointPair>> matches = pointsGrouped.flatMapGroups(new FlatMapGroupsFunction<Integer, Tuple2<Integer, GeoObject>, Tuple2<Integer, PointPair>>() {
			@Override
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

			@Override
			public Iterator<String> call(Tuple2<Integer, PointPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput.iterator();
			}
		
		}, Encoders.STRING()).repartition(amountPartition);
		
		return output;
	}
}
