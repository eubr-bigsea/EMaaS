package PolygonMatching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

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
	private static String    DATA_FOLDER;
	private static String[]    filePaths;

	public static void main(String[] args) throws Exception {
		
		/*
		String dataSource1 = args[0];
		String dataSource2 = args[1];
		double thresholdLinguistic = Double.parseDouble(args[2]);
		double thresholdPolygon = Double.parseDouble(args[3]);
		String outputPath = args[4];
		Integer numFrag = Integer.parseInt(args[5]);
		String sourceType = args[6];
		*/
		
		String dataSource1 = "bus_data/polygons_data/squares_pref_curitiba.csv";
		String dataSource2 = "bus_data/polygons_data/osm_curitiba.csv";
		double thresholdLinguistic = 0.7d;
		double thresholdPolygon = 0.7d;
		//String outputPath = args[4];
		Integer numFrag = 1;
		String sourceType = "CSV";
		
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
		
		
		List<GeoPolygon> polygonsTarget = new ArrayList<GeoPolygon>();
		List<GeoPolygon> polygonsSource = new ArrayList<GeoPolygon>();
		
		// the algorithm returns each generated pair step-by-step
		int indexOfPref = 0;
		for (GenericObject genericObj : storagePref.getExtractedData()) {
			String nome = "";
			Integer id;
			if (!genericObj.getData().get("name").toString().equals("null")) {//for curitiba use atribute "nome" for new york "signname"
				nome = genericObj.getData().get("name").toString();
				id = Integer.parseInt(genericObj.getData().get("id").toString());//for curitiba use atribute "gid" for new york "id"
				polygonsTarget.add(new GeoPolygon(genericObj.getData().get("geometry").toString(), nome, InputTypes.GOV_POLYGON, indexOfPref, id));
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
				polygonsSource.add(new GeoPolygon(genericObj.getData().get("geometry").toString(), nome, InputTypes.OSM_POLYGON, indexOfOSM, id));
				indexOfOSM++;
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
				
//				int index = entityMatches.size();
//				entityMatches.add(new Tuple2<Integer, PolygonPair>(index, pair));
				
				//for use case 04
				if (pair.getPolygonClassification().equals(PolygonClassification.POSSIBLE_PROBLEM) || pair.getPolygonClassification().equals(PolygonClassification.MATCH)) {
					int index = entityMatches.size();
					entityMatches.add(new Tuple2<Integer, PolygonPair>(index, pair));
				}
				
				/*
				filePaths = new String[numFrag];
				List<Tuple2<Integer, PolygonPair>> result = new ArrayList<Tuple2<Integer, PolygonPair>>();

				for (int i = 0; i < numFrag; i++){
					filePaths[i] = DATA_FOLDER+"_"+String.format("%02d", i);
					//HashMap<String, Integer> partialResult = map(filePaths[i]);
					result = mergeResults(entityMatches, result);
				}*/
//					if (Math.abs(entTarget.getArea() - entSource.getArea()) > thresholdArea) {
//						entityMatches.add(new Tuple2<String, String>(entTarget.getGeoName(), entSource.getGeoName() + ":" + Math.abs(entTarget.getArea() - entSource.getArea())));
////						System.out.println(entTarget.getGeoName() +  " - " + entSource.getGeoNameame(), _2));
////						System.out.println(entTarget.getGeoName() +  " - " + ());
////						System.out.println(entTarget.getGeoName() + " pref: " + String.format("%.2f", entTarget.getArea()));
////						System.out.println(entSource.getGeoName() + " OSM: " + String.format("%.2f", entSource.getArea()));
////						System.out.println();
//					}
			}
		}
		
		System.out.println("[LOG] Result size = " + entityMatches.size());
		
		for (Tuple2<Integer, PolygonPair> tuple : entityMatches) {
			System.out.println(tuple._2().toStringCSV());
		}
	}
	
	public static List<Tuple2<Integer, PolygonPair>> mergeResults(List<Tuple2<Integer, PolygonPair>> m1, List<Tuple2<Integer, PolygonPair>> m2) {
		Iterator<Tuple2<Integer, PolygonPair>> it1 = m1.iterator();
		while (it1.hasNext()) {
			Tuple2<Integer, PolygonPair> pair = (Tuple2<Integer, PolygonPair>)it1.next();
			m2.add(pair);
			it1.remove(); // avoids a ConcurrentModificationException
		}

		return m2;
	}
}