package PolygonMatching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;

import PolygonDependencies.GeoPolygon;
import PolygonDependencies.InputTypes;
import PolygonDependencies.PolygonClassification;
import PolygonDependencies.PolygonPair;
import genericEntity.datasource.DataSource;
import genericEntity.exec.AbstractExec;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.storage.StorageManager;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scala.Tuple2;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;

public final class MatchingGeoPolygon {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 

	public static void main(String[] args) throws Exception {
//		SparkConf sparkConf = new SparkConf().setAppName("GeoMatchingSpark").setMaster("local");
		SparkConf sparkConf = new SparkConf().setAppName("GeoMatchingSpark");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);
		
		String dataSource1 = args[0];
		String dataSource2 = args[1];
		final double thresholdLinguistic = Double.parseDouble(args[2]);
		final double thresholdPolygon = Double.parseDouble(args[3]);
		String outputPath = args[4];
		Integer amountPartition = Integer.parseInt(args[5]);
		String sourceType = args[6];
		
		
		DataSource dataSourcePref = null;
		DataSource dataSourceOSM = null;
		if (sourceType.equals("CSV")) {
			dataSourcePref = AbstractExec.getDataCSV(dataSource1, ';');
			dataSourceOSM = AbstractExec.getDataCSV(dataSource2, ';');
		} else { //is postgis
			dataSourcePref = AbstractExec.getDataPostGres(dataSource1);
			dataSourceOSM = AbstractExec.getDataPostGres(dataSource2);
		}

//		DataSource dataSourcePref = AbstractExec.getDataPostGres(dataSource1); //squaresOfCuritiba Pref
//		DataSource dataSourceOSM = AbstractExec.getDataPostGres(dataSource2); //squaresOfCuritiba OSM
		
//		DataSource dataSourcePref = AbstractExec.getDataPostGres("queries/squares_pref_curitiba.txt"); //squaresOfCuritiba Pref
//		DataSource dataSourceOSM = AbstractExec.getDataPostGres("queries/osm_curitiba.txt"); //squaresOfCuritiba OSM

//		DataSource dataSourcePref = AbstractExec.getDataPostGres("queries/parks_pref_ny.txt"); //parksOfNY Pref
//		DataSource dataSourceOSM = AbstractExec.getDataPostGres("queries/osm_ny.txt"); //parksOfNY OSM
		
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
//					System.out.println(genericObj.getData().get("geometry"));
			String nome = "";
			Integer id;
			if (!genericObj.getData().get("name").toString().equals("null")) {
				nome = genericObj.getData().get("name").toString();
				id = Integer.parseInt(genericObj.getData().get("id").toString());
				geoentitiesOSM.add(new GeoPolygon(genericObj.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON, indexOfOSM, id));
				indexOfOSM++;
			}
			
		}
		
		JavaRDD<GeoPolygon> polygonsOSM = ctx.parallelize(geoentitiesOSM);
		JavaRDD<GeoPolygon> polygonsPref = ctx.parallelize(geoentitiesPref);
		
		JavaRDD<GeoPolygon> polygons = polygonsPref.union(polygonsOSM);
		
		final Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		JavaRDD<Tuple2<Integer, GeoPolygon>> polygonLabed = polygons.flatMap(new FlatMapFunction<GeoPolygon, Tuple2<Integer, GeoPolygon>>() {

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
			
		});
		
		JavaPairRDD<Integer, GeoPolygon> polygonsPaired = polygonLabed.mapToPair(new PairFunction<Tuple2<Integer,GeoPolygon>, Integer, GeoPolygon>() {

			public Tuple2<Integer, GeoPolygon> call(Tuple2<Integer, GeoPolygon> tuple) throws Exception {
				return new Tuple2<Integer, GeoPolygon>(tuple._1(), tuple._2());
			}
		});
		
		JavaPairRDD<Integer, Iterable<GeoPolygon>> polygonsGrouped = polygonsPaired.groupByKey(amountPartition);//number of partitions
		
		JavaPairRDD<Integer, PolygonPair> matches = polygonsGrouped.flatMapToPair(new PairFlatMapFunction<Tuple2<Integer,Iterable<GeoPolygon>>, Integer, PolygonPair>() {

			public Iterator<Tuple2<Integer, PolygonPair>> call(Tuple2<Integer, Iterable<GeoPolygon>> tuple) throws Exception {
				List<GeoPolygon> polygonsPerKey = IteratorUtils.toList(tuple._2().iterator());
				List<GeoPolygon> polygonsSource = new ArrayList<GeoPolygon>();
				List<GeoPolygon> polygonsTarget = new ArrayList<GeoPolygon>();
				for (GeoPolygon entity : polygonsPerKey) {
					if (entity.getType() == InputTypes.OSM_POLYGON) {
						polygonsSource.add(entity);
					} else {
						polygonsTarget.add(entity);
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
		});
		
		matches.flatMap(new FlatMapFunction<Tuple2<Integer, PolygonPair>, String>() {

			public Iterator<String> call(Tuple2<Integer, PolygonPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput.iterator();
			}
		
		}).saveAsTextFile(outputPath);
		
		
		ctx.stop();
		ctx.close();
	}
	
	
}