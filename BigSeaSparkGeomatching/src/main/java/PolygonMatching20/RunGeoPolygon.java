package PolygonMatching20;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import PointDependencies.FieldsInputsMatchUp;
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
		
		Dataset<Row> dataset1 = spark.read().text(dataSource1);
		Dataset<Row> dataset2 = spark.read().text(dataSource2);
		
		FieldsInputsMatchUp headerPref = new FieldsInputsMatchUp("geometry","name", "indexOfID","id");
		FieldsInputsMatchUp headerOSM = new FieldsInputsMatchUp("geometry","name", "indexOfID","id");
		
		MatchingGeoPolygon20 mp = new MatchingGeoPolygon20();
		Dataset<GeoPolygon> polygons = MatchingGeoPolygon20.generateDataFrames(dataset1, dataset2, headerPref, headerOSM, spark);
		MatchingGeoPolygon20.run(polygons, thresholdLinguistic, thresholdPolygon, amountPartition, spark).javaRDD().saveAsTextFile(outputPath);

	}

}
