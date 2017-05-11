package PolygonMatching20;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

import PolygonDependencies.GeoPolygon;

public class RunGeoPolygon {

	public static void main(String[] args) throws Exception {
		String dataSource1 = args[0];
		String dataSource2 = args[1];
		double thresholdLinguistic = Double.parseDouble(args[2]);
		double thresholdPolygon = Double.parseDouble(args[3]);
		String outputPath = args[4];
		Integer amountPartition = Integer.parseInt(args[5]);
		String sourceType = args[6];
		
		
//		SparkConf sparkConf = new SparkConf().setAppName("GeoMatchingSpark").setMaster("local");
		SparkSession spark = SparkSession
				  .builder()
				  .master("local")
				  .config("spark.some.config.option", "some-value")
				  .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
				  .getOrCreate();
		
		
		MatchingGeoPolygon20 mp = new MatchingGeoPolygon20();
		Dataset<GeoPolygon> polygons = MatchingGeoPolygon20.generateDataFrames(dataSource1, dataSource2, sourceType, spark);
		MatchingGeoPolygon20.run(polygons, thresholdLinguistic, thresholdPolygon, amountPartition, spark).javaRDD().saveAsTextFile(outputPath);

	}

}
