package SingleMatchingGeoPolygon;

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
import genericEntity.exec.AbstractExec;
import genericEntity.input.ReadAbstractSource;
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

public final class SingleMatchingGeoPolygonBlocked {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 

	public static void main(String[] args) throws Exception {
		SparkConf sparkConf = new SparkConf().setAppName("GeoMatchingSpark").setMaster("local");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);
		
		String dataSource = args[0];
		double thresholdLinguistic = Double.parseDouble(args[1]);
		double thresholdPolygon = Double.parseDouble(args[2]);
		String outputPath = args[3];
		Integer amountPartition = Integer.parseInt(args[4]);
		
		
		ReadAbstractSource reader = new ReadAbstractSource();
		StorageManager storagePolygon = reader.readFile(AbstractExec.getDataPostGres(dataSource));
//		StorageManager storagePolygon = reader.readFile(AbstractExec.getDataPostGres("queries/osm_curitiba.txt"));
//		StorageManager storagePolygon = reader.readFile(AbstractExec.getDataPostGres("queries/squares_pref_curitiba.txt"));
		
		List<GeoPolygon> geoentities = new ArrayList<GeoPolygon>();
		
		int index = 0;
		for (GenericObject genericObj : storagePolygon.getExtractedData()) {
//					System.out.println(genericObj.getData().get("geometry"));
			String nome = "";
			Integer id;
			if (!genericObj.getData().get("name").toString().equals("null")) {
				nome = genericObj.getData().get("name").toString();
				id = Integer.parseInt(genericObj.getData().get("id").toString());
				geoentities.add(new GeoPolygon(genericObj.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON, index, id));
				index++;
			}
		}
		
		JavaRDD<GeoPolygon> polygons = ctx.parallelize(geoentities);
		
		Broadcast<Integer> numReplication = ctx.broadcast(amountPartition);
		JavaRDD<Tuple2<String, GeoPolygon>> polygonLabed = polygons.flatMap(new FlatMapFunction<GeoPolygon, Tuple2<String, GeoPolygon>>() {

			@Override
			public List<Tuple2<String, GeoPolygon>> call(GeoPolygon s) throws Exception {
				List<Tuple2<String, GeoPolygon>> listOfPolygonTuple = new ArrayList<Tuple2<String, GeoPolygon>>();
				GeoPolygon tocompare = s.getGeoPolygon();
				tocompare.setDuplicated(false);
				if (tocompare.getGeoName().length() < 3) {
					listOfPolygonTuple.add(new Tuple2<String, GeoPolygon>(tocompare.getGeoName(), tocompare));//entity that not replicated
				} else {
					listOfPolygonTuple.add(new Tuple2<String, GeoPolygon>(tocompare.getGeoName().substring(0, 3), tocompare));//entity that not replicated
				}
				
				
				GeoPolygon duplicated = s.getGeoPolygon();
				duplicated.setDuplicated(true);
				if (duplicated.getGeoName().length() < 3) {
					listOfPolygonTuple.add(new Tuple2<String, GeoPolygon>(duplicated.getGeoName(), duplicated));
				} else {
					listOfPolygonTuple.add(new Tuple2<String, GeoPolygon>(duplicated.getGeoName().substring(0, 3), duplicated));
				}
				
//				for (int i = 0; i < numReplication.value(); i++) {//the entities that will be replicated
//					listOfPolygonTuple.add(new Tuple2<Integer, GeoPolygon>(duplicated, duplicated));
//				}
				return listOfPolygonTuple;
			}
			
		});
		
		JavaPairRDD<String, GeoPolygon> polygonsPaired = polygonLabed.mapToPair(new PairFunction<Tuple2<String,GeoPolygon>, String, GeoPolygon>() {

			@Override
			public Tuple2<String, GeoPolygon> call(Tuple2<String, GeoPolygon> tuple) throws Exception {
				return new Tuple2<String, GeoPolygon>(tuple._1(), tuple._2());
			}
		});
		
		JavaPairRDD<String, Iterable<GeoPolygon>> polygonsGrouped = polygonsPaired.groupByKey(amountPartition);//number of partitions
		
		JavaPairRDD<Integer, PolygonPair> matches = polygonsGrouped.flatMapToPair(new PairFlatMapFunction<Tuple2<String,Iterable<GeoPolygon>>, Integer, PolygonPair>() {

			@Override
			public List<Tuple2<Integer, PolygonPair>> call(Tuple2<String, Iterable<GeoPolygon>> tuple) throws Exception {
				List<GeoPolygon> polygonsPerKey = IteratorUtils.toList(tuple._2().iterator());
				List<GeoPolygon> polygonsToCompare = new ArrayList<GeoPolygon>();
				List<GeoPolygon> polygonsDuplicated = new ArrayList<GeoPolygon>();
				for (GeoPolygon entity : polygonsPerKey) {
					if (entity.isDuplicated()) {
						polygonsDuplicated.add(entity);
					} else {
						polygonsToCompare.add(entity);
					}
				}
				
				List<Tuple2<Integer, PolygonPair>> entityMatches = new ArrayList<Tuple2<Integer, PolygonPair>>();
				JaccardSimilarity jaccard = new JaccardSimilarity();
				for (GeoPolygon entSource : polygonsToCompare) {
					for (GeoPolygon entTarget : polygonsDuplicated) {
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
						if (pair.getPolygonClassification().equals(PolygonClassification.MATCH) && (pair.getSource().getIdInDataset() != pair.getTarget().getIdInDataset())) {
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
				return entityMatches;
			}
		});
		
		matches.flatMap(new FlatMapFunction<Tuple2<Integer, PolygonPair>, String>() {

			@Override
			public ArrayList<String> call(Tuple2<Integer, PolygonPair> t) throws Exception {
				ArrayList<String> listOutput = new ArrayList<String>();
				listOutput.add(t._2().toStringCSV());
				return listOutput;
			}
		
		}).saveAsTextFile(outputPath);
		
		
		ctx.stop();
		ctx.close();
	}
}