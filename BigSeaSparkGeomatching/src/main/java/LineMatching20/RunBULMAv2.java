package LineMatching20;

import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import LineDependencies.GeoLine;
import scala.Tuple2;

public class RunBULMAv2 {

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Usage: <shape file> <GPS file> <directory of output path> <number of partitions>");
			System.exit(1);
		}

		Long initialTime = System.currentTimeMillis();
		
		String pathFileShapes = args[0];
		String pathGPSFile = args[1];
		String pathOutput = args[2];
		int minPartitions = Integer.valueOf(args[3]);
		
		
		SparkSession spark = SparkSession
				  .builder()
				  .master("local")
				  .config("spark.some.config.option", "some-value")
				  .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
				  .getOrCreate();
			
		Dataset<Row> datasetGPSFile = spark.read().text(pathGPSFile);
		Row headerGPSFile = datasetGPSFile.first();
		datasetGPSFile = datasetGPSFile.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean call(Row value) throws Exception {
				if (value.equals(headerGPSFile)) {
					return false;
				}
				return true;
			}
		});
		
		
		Dataset<Row> datasetShapesFile = spark.read().text(pathFileShapes);
		Row headerShapesFile = datasetShapesFile.first();
		datasetShapesFile = datasetShapesFile.filter(new FilterFunction<Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean call(Row value) throws Exception {
				if (value.equals(headerShapesFile)) {
					return false;
				}
				return true;
			}
		});
		
		Dataset<Tuple2<String, GeoLine>> lines = MatchingRoutesV2.generateDataFrames(datasetShapesFile, datasetGPSFile, minPartitions, spark);
		Dataset<String> output = MatchingRoutesV2.run(lines,minPartitions, spark);
		output.toJavaRDD().saveAsTextFile(pathOutput);
		
		System.out.println("Execution time with Dataset: " + (System.currentTimeMillis() - initialTime));
		
	}

}
