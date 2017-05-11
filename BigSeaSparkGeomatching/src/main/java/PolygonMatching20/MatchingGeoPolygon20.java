package PolygonMatching20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
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
import PolygonDependencies.GeoPolygon;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import PolygonDependencies.PolygonPair;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;
import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingGeoPolygon20 {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 
	
	public static Dataset<GeoPolygon> generateDataFrames(String dataSource1, String dataSource2, String sourceType, SparkSession spark) throws Exception {
		DataSource dataSourcePref = null;
		DataSource dataSourceOSM = null;
		if (sourceType.equals("CSV")) {
			dataSourcePref = AbstractExec.getDataCSV(dataSource1, ';');
			dataSourceOSM = AbstractExec.getDataCSV(dataSource2, ';');
		} else { //is postgis
			dataSourcePref = AbstractExec.getDataPostGres(dataSource1);
			dataSourceOSM = AbstractExec.getDataPostGres(dataSource2);
		}

        StorageManager storagePref = new StorageManager();
        StorageManager storageOSM = new StorageManager();
		
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
		
		
		List<GeoPolygon> geoentitiesPref = new ArrayList<GeoPolygon>();
		List<GeoPolygon> geoentitiesOSM = new ArrayList<GeoPolygon>();
		
		// the algorithm returns each generated pair step-by-step
		int indexOfPref = 0;
		for (GenericObject genericObj : storagePref.getExtractedData()) {
			String nome = "";
			Integer id;
			if (!genericObj.getData().get("name").toString().equals("null")) {//for curitiba use atribute "nome" for new york "signname"
				nome = genericObj.getData().get("name").toString();
				id = Integer.parseInt(genericObj.getData().get("id").toString());//for curitiba use atribute "gid" for new york "id"
				geoentitiesPref.add(new GeoPolygon(genericObj.getData().get("geometry").toString(), nome, InputTypes.GOV_POLYGON, indexOfPref, id));
				indexOfPref++;
			}
			
		}
		
		
		int indexOfOSM = 0;
		for (GenericObject genericObj : storageOSM.getExtractedData()) {
			String nome = "";
			Integer id;
			if (!genericObj.getData().get("name").toString().equals("null")) {
				nome = genericObj.getData().get("name").toString();
				id = Integer.parseInt(genericObj.getData().get("id").toString());
				geoentitiesOSM.add(new GeoPolygon(genericObj.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON, indexOfOSM, id));
				indexOfOSM++;
			}
			
		}
		
//		Dataset<Row> squaresDF = spark.createDataFrame(geoentitiesOSM, GeoPolygon.class);
//		squaresDF.show();
		
//		Encoder<GeoPolygon> polygonEncoder = Encoders.bean(GeoPolygon.class);
		Encoder<GeoPolygon> polygonEncoder = Encoders.javaSerialization(GeoPolygon.class);
		
		Dataset<GeoPolygon> polygonsOSM = spark.createDataset(geoentitiesOSM, polygonEncoder);
		Dataset<GeoPolygon> polygonsPref = spark.createDataset(geoentitiesPref, polygonEncoder);
		
		Dataset<GeoPolygon> polygons = polygonsPref.union(polygonsOSM);
		
		return polygons;
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