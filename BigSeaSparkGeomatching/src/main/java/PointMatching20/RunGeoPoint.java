package PointMatching20;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import BULMADependences.GeoObject;
import PointDependencies.FieldsInputsMatchUp;

public class RunGeoPoint {

	public static void main(String[] args) throws Exception {
		String dataSource1 = args[0];
		String dataSource2 = args[1];
		String dataSourceContext = args[2];
		double thresholdLinguistic = Double.parseDouble(args[3]);
		double thresholdDistance = Double.parseDouble(args[4]);
		String outputPath = args[5];
		Integer amountPartition = Integer.parseInt(args[6]);
		String sourceType = args[7];
		
		
//		SparkConf sparkConf = new SparkConf().setAppName("GeoMatchingSpark").setMaster("local");
		SparkSession spark = SparkSession
				  .builder()
				  .master("local")
				  .config("spark.some.config.option", "some-value")
				  .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
				  .getOrCreate();
		
		
		Dataset<Row> dataset1 = spark.read().text(dataSource1);
		Dataset<Row> dataset2 = spark.read().text(dataSource2);
		Dataset<Row> dataset3 = spark.read().text(dataSourceContext);
		
		FieldsInputsMatchUp headerBusStops_pref = new FieldsInputsMatchUp("geometry","name", "indexOfID","id");
		FieldsInputsMatchUp headerBusStops_osm = new FieldsInputsMatchUp("geometry","name", "indexOfID","id");
		FieldsInputsMatchUp headerBusStops_street = new FieldsInputsMatchUp("geometry","name", "indexOfID","id");
		
//		Dataset<GeoObject> busStops = ContextMatchingBusStops20.generateDataFrames(dataset1, dataset2, dataset3, headerBusStops_pref, headerBusStops_osm, headerBusStops_street, spark);
//		ContextMatchingBusStops20.run(busStops, thresholdLinguistic, thresholdDistance, amountPartition, spark).javaRDD().saveAsTextFile(outputPath);
		
		Dataset<GeoObject> busStops = MatchingBusStops20.generateDataFrames(dataset1, dataset2, headerBusStops_pref, headerBusStops_osm, spark);
		MatchingBusStops20.run(busStops, thresholdLinguistic, thresholdDistance, amountPartition, spark).javaRDD().saveAsTextFile(outputPath);

	}

}
