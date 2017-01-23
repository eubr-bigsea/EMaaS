package PointMatching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.Accumulator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;

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

public final class MatchingBusStops {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 

	public static void main(String[] args) throws Exception {
		SparkConf sparkConf = new SparkConf().setAppName("JavaDeduplication").setMaster("local");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);
		
		double thresholdLinguistic = Double.parseDouble(args[0]);
		double thresholdPointDistance = Double.parseDouble(args[1]);
		String outputPath = args[2];
		Integer amountPartition = Integer.parseInt(args[3]);
		
		/*
		if (args.length < 1) {
			System.err.println("Usage: MatchingGeoPoints <file>");
			System.exit(1);
		}*/
		
		DataSource dataSourcePref = AbstractExec.getDataPostGres("queries/bustops_pref_curitiba2.txt"); //busStops Pref
//		DataSource dataSourcePref = AbstractExec.getDataPostGres("queries/bustops_osm_curitiba.txt"); //busStops OSM
		
//		DataSource dataSourceOSM = AbstractExec.getDataPostGres("queries/bustops_pref_curitiba2.txt"); //busStops OSM
		DataSource dataSourceOSM = AbstractExec.getDataPostGres("queries/bustops_osm_curitiba.txt"); //busStops OSM
		
		StorageManager storagePref = new StorageManager();
        StorageManager storageOSM = new StorageManager();
		
		// enables in-memory execution for faster processing
		// this can be done since the whole data fits into memory
        storagePref.enableInMemoryProcessing();
        storageOSM.enableInMemoryProcessing();

		// adds the "data" to the algorithm
        storagePref.addDataSource(dataSourcePref);
        storageOSM.addDataSource(dataSourceOSM);

		if(!storagePref.isDataExtracted()) {
			storagePref.extractData();
		}
		if(!storageOSM.isDataExtracted()) {
			storageOSM.extractData();
		}
		
		
		List<GeoPoint2> geoPointsPref = new ArrayList<GeoPoint2>();
		List<GeoPoint2> geoPointsOSM = new ArrayList<GeoPoint2>();
		
		// the algorithm returns each generated pair step-by-step
		int indexOfPref = 0;
		for (GenericObject dude : storagePref.getExtractedData()) {
			String nome = "";
			Integer id;
//			if (!dude.getData().get("name").toString().equals("null")) {//for curitiba use atribute "nome" for new york "signname"
				nome = dude.getData().get("name").toString();
				id = Integer.parseInt(dude.getData().get("id").toString());//for curitiba use atribute "gid" for new york "id"
				geoPointsPref.add(new GeoPoint2(dude.getData().get("geometry").toString(), nome, InputTypes.GOV_POLYGON, indexOfPref, id));
				indexOfPref++;
//			}
			
		}
		
		
		int indexOfOSM = 0;
		for (GenericObject dude : storageOSM.getExtractedData()) {
//					System.out.println(dude.getData().get("geometry"));
			String nome = "";
			Integer id;
//			if (!dude.getData().get("name").toString().equals("null")) {
				nome = dude.getData().get("name").toString();
				id = Integer.parseInt(dude.getData().get("id").toString());
				geoPointsOSM.add(new GeoPoint2(dude.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON, indexOfOSM, id));
				indexOfOSM++;
//			}
			
		}
		
		JavaRDD<GeoPoint2> pointsOSM = ctx.parallelize(geoPointsOSM);
		JavaRDD<GeoPoint2> pointsPref = ctx.parallelize(geoPointsPref);
		
		JavaRDD<GeoPoint2> points = pointsPref.union(pointsOSM);

		Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		JavaRDD<Tuple2<Integer, GeoPoint2>> pointLabed = points.flatMap(new FlatMapFunction<GeoPoint2, Tuple2<Integer, GeoPoint2>>() {

			@Override
			public Iterable<Tuple2<Integer, GeoPoint2>> call(GeoPoint2 s) throws Exception {
				List<Tuple2<Integer, GeoPoint2>> listOfPointTuple = new ArrayList<Tuple2<Integer, GeoPoint2>>();
				if (s.getType().equals(InputTypes.OSM_POLYGON)) {
					listOfPointTuple.add(new Tuple2<Integer, GeoPoint2>(s.getIdGeometry()%numReplication.getValue(), s));
					return listOfPointTuple;
				} else { //equals to InputTypes.GOV_POLYGON
					for (int i = 0; i < numReplication.value(); i++) {
						listOfPointTuple.add(new Tuple2<Integer, GeoPoint2>(i, s));
					}
					return listOfPointTuple;
				}
			}
			
		});
		
		JavaPairRDD<Integer, GeoPoint2> pointsPaired = pointLabed.mapToPair(new PairFunction<Tuple2<Integer,GeoPoint2>, Integer, GeoPoint2>() {

			@Override
			public Tuple2<Integer, GeoPoint2> call(Tuple2<Integer, GeoPoint2> tuple) throws Exception {
				return new Tuple2<Integer, GeoPoint2>(tuple._1(), tuple._2());
			}
		});
		
		JavaPairRDD<Integer, Iterable<GeoPoint2>> pointsGrouped = pointsPaired.groupByKey(amountPartition);//number of partitions
		
		Accumulator<Double> accum = ctx.accumulator(0.0);
		
		JavaPairRDD<Integer, PointPair> matches = pointsGrouped.flatMapToPair(new PairFlatMapFunction<Tuple2<Integer,Iterable<GeoPoint2>>, Integer, PointPair>() {

			@Override
			public Iterable<Tuple2<Integer, PointPair>> call(Tuple2<Integer, Iterable<GeoPoint2>> tuple) throws Exception {
				List<GeoPoint2> pointsPerKey = IteratorUtils.toList(tuple._2().iterator());
				List<GeoPoint2> pointsSource = new ArrayList<GeoPoint2>();
				List<GeoPoint2> pointsTarget = new ArrayList<GeoPoint2>();
				for (GeoPoint2 entity : pointsPerKey) {
					if (entity.getType() == InputTypes.OSM_POLYGON) {
						pointsSource.add(entity);
					} else {
						pointsTarget.add(entity);
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
						double distanceSimilarity = entSource.getPointDistanceInMeters(entTarget);
						
						//classification of pairs
						PointPair pair;
						if (distanceSimilarity <= thresholdPointDistance /*&& linguisticSimilarity > thresholdLinguistic*/ && (!entSource.getIdInDataset().equals(entTarget.getIdInDataset()))) {
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
				return entityMatches;
			}
		});
		
		matches.flatMap(new FlatMapFunction<Tuple2<Integer, PointPair>, String>() {

			@Override
			public Iterable<String> call(Tuple2<Integer, PointPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput;
			}
		
		}).saveAsTextFile(outputPath);
		
		System.out.println(accum.value());
		
		ctx.stop();
		ctx.close();
	}
}
