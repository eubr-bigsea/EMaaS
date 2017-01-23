package PointMatching;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import PointDependencies.ClosestPoints;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import PointDependencies.StopPoint;

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

public final class MatchingGeoPoints {
	
	private static final Pattern SPACE = Pattern.compile(" ");
	private static final int rangeBlockingKey = 7; 

	public static void main(String[] args) throws Exception {
		
		/*
		if (args.length < 1) {
			System.err.println("Usage: MatchingGeoPoints <file>");
			System.exit(1);
		}*/
		
		String textFile = "C:/Users/Brasileiro/Google Drive/Bigsea (Pessoal)/GeoMatching/Linhas de Ônibus Curitiba/shape_line_022.csv";
		String textFile2 = "C:/Users/Brasileiro/Google Drive/Bigsea (Pessoal)/GeoMatching/Linhas de Ônibus Curitiba/gps_line_022.csv";

		SparkConf sparkConf = new SparkConf().setAppName("JavaDeduplication").setMaster("local");
		JavaSparkContext ctx = new JavaSparkContext(sparkConf);
		
		JavaRDD<String> shapePoints = ctx.textFile(textFile, 1);
		JavaRDD<String> gpsPoints = ctx.textFile(textFile2, 1);

		@SuppressWarnings("serial")
		JavaRDD<GeoPoint> shapePointsRDD = shapePoints.flatMap(new FlatMapFunction<String, GeoPoint>() {
			public Iterable<GeoPoint> call(String s) {
				return Arrays.asList((GeoPoint)ShapePoint.createShapePoint(s));
			}
		});
		
		@SuppressWarnings("serial")
		JavaRDD<GeoPoint> gpsPointsRDD = gpsPoints.flatMap(new FlatMapFunction<String, GeoPoint>() {
			public Iterable<GeoPoint> call(String s) {
				return Arrays.asList((GeoPoint)GPSPoint.createGPSPoint(s));
			}
		});
		
		JavaRDD<GeoPoint> points = gpsPointsRDD.union(shapePointsRDD);
		
		@SuppressWarnings("serial")
		JavaPairRDD<String, GeoPoint> ones = points.mapToPair(new PairFunction<GeoPoint, String, GeoPoint>() {
			public Tuple2<String, GeoPoint> call(GeoPoint s) {
				if ((s instanceof GPSPoint) || (s instanceof StopPoint)) {
					s.addFirst();
				}
				return new Tuple2<String, GeoPoint>(s.getBlockingKey(rangeBlockingKey), s);
			}
		}).sortByKey();
		
//		List<Tuple2<String, GeoPoint>> list = ones.collect();
//		for (Tuple2<?,?> tuple : list) {
//			System.out.println(tuple._1().toString() +  " - " + tuple._2().toString());
//		}
		
		
		@SuppressWarnings("serial")
		JavaPairRDD<String, GeoPoint> geoPoints = ones.reduceByKey(new Function2<GeoPoint, GeoPoint, GeoPoint>() {
			private GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();
			private Geometry interestPoint = geometryFactory.createPoint(new Coordinate(0.0, 0.0));
			private Geometry comparisonPoint = geometryFactory.createPoint(new Coordinate(0.0, 0.0));
			
			@Override
			public GeoPoint call(GeoPoint point1, GeoPoint point2) throws Exception {
				if (!(point1 instanceof GPSPoint) && !(point1 instanceof StopPoint)) {
					return point1;
				}
				
				if (point2 instanceof GPSPoint || point2 instanceof StopPoint){
					point1.addGeoPoint(point2);
				} else {
					List<ClosestPoints> arrayGPSPoints = point1.getAcumulator();
					comparisonPoint = geometryFactory.createPoint(new Coordinate(Double.valueOf(point2.getLatitude()), Double.valueOf(point2.getLongitude())));
					for (ClosestPoints gpsPoint : arrayGPSPoints) {
						interestPoint = geometryFactory.createPoint(new Coordinate(Double.valueOf(gpsPoint.getPoint1().getLatitude()), Double.valueOf(gpsPoint.getPoint1().getLongitude())));
						double newDistance = interestPoint.distance(comparisonPoint);
						if(gpsPoint.getClosestDistance() == -1 || newDistance < gpsPoint.getClosestDistance()){
							gpsPoint.setPoint2(point2);
							gpsPoint.setClosestDistance(newDistance);	
						}
					}
					point1.setAcumulator(arrayGPSPoints);
				}
				return point1;
			}
		
		});
		
		List<Tuple2<String, GeoPoint>> output = geoPoints.collect();
		for (Tuple2<?,?> tuple : output) {
			if (!tuple._1().equals("unuseful")){
				
				@SuppressWarnings("unchecked")
				GeoPoint closestPoints = (GeoPoint) tuple._2();
				DecimalFormat df = new DecimalFormat("#.00000000"); 
				
				for (ClosestPoints cp : closestPoints.getAcumulator()) {
					
					//If any distance appear with -1.0 it means that there aren't any (shape) point closer to the interest point inside the range of the defined blocking key. 
					System.out.printf("Na Chave de bloco: " + tuple._1() + ", " +
							"O ponto de interesse(GPS) " + cp.getPoint1().getLatitude() + "/" + cp.getPoint1().getLongitude() + 
							" possui o ponto (SHAPE) " + cp.getPoint2().getLatitude() + "/" + cp.getPoint2().getLongitude() + 
							" como ponto mais próximo com a distância de " + "%.5f", cp.getClosestDistance());
					System.out.println();
				}
			}
		}
		
		
		
		ctx.stop();
		ctx.close();
	}
}