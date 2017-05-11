package PointMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.Accumulator;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.KeyValueGroupedDataset;
import org.apache.spark.sql.SparkSession;

import LineDependencies.GeoObject;
import PointDependencies.GeoPoint2;
import PointDependencies.PointPair;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingBusStops20 {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 
	
	public static Dataset<GeoObject> generateDataFrames(String dataSource1, String dataSource2, String sourceType, SparkSession spark) throws Exception {
		DataSource source1 = null;
		DataSource source2 = null;
		if (sourceType.equals("CSV")) {
			source1 = AbstractExec.getDataCSV(dataSource1, ';');
			source2 = AbstractExec.getDataCSV(dataSource2, ';');
		} else { // is postgis
			source1 = AbstractExec.getDataPostGres(dataSource1);
			source2 = AbstractExec.getDataPostGres(dataSource2);
		}
		
		
		StorageManager storageDS1 = new StorageManager();
		StorageManager storageDS2 = new StorageManager();

		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
		storageDS1.enableInMemoryProcessing();
		storageDS2.enableInMemoryProcessing();

		// adds the "data" to the algorithm
		storageDS1.addDataSource(source1);
		storageDS2.addDataSource(source2);

		if (!storageDS1.isDataExtracted()) {
			storageDS1.extractData();
		}
		if (!storageDS2.isDataExtracted()) {
			storageDS2.extractData();
		}

		List<GeoObject> geoPointsDS1 = new ArrayList<GeoObject>();
		List<GeoObject> geoPointsDS2 = new ArrayList<GeoObject>();

		// the algorithm returns each generated pair step-by-step
		int indexOfPref = 0;
		for (GenericObject dude : storageDS1.getExtractedData()) {
			String nome = "";
			Integer id;
			// if (!dude.getData().get("name").toString().equals("null")) {//for
			// curitiba use atribute "nome" for new york "signname"
			nome = new String(dude.getData().get("name").toString().getBytes("UTF8"), "UTF8");
			id = Integer.parseInt(dude.getData().get("id").toString());// for curitiba use atribute "gid", for new york "id"

			if (!dude.getData().get("geometry").toString().contains("INF")) {
				geoPointsDS1.add(new GeoPoint2(dude.getData().get("geometry").toString(), nome, InputTypes.GOV_POLYGON,
						indexOfPref, id));
				indexOfPref++;
			}
			// }

		}

		int indexOfOSM = 0;
		for (GenericObject dude : storageDS2.getExtractedData()) {
			// System.out.println(dude.getData().get("geometry"));
			String nome = "";
			Integer id;
			// if (!dude.getData().get("name").toString().equals("null")) {
			nome = new String(dude.getData().get("name").toString().getBytes(), "UTF-8");
			id = Integer.parseInt(dude.getData().get("id").toString());
			geoPointsDS2.add(new GeoPoint2(dude.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON,
					indexOfOSM, id));
			indexOfOSM++;
			// }

		}

		Encoder<GeoObject> geoObjEncoder = Encoders.javaSerialization(GeoObject.class);
		
		Dataset<GeoObject> pointsDS1 = spark.createDataset(geoPointsDS1, geoObjEncoder);
		Dataset<GeoObject> pointsDS2 = spark.createDataset(geoPointsDS2, geoObjEncoder);
		
		Dataset<GeoObject> points = pointsDS1.union(pointsDS2);
		
		return points;
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
