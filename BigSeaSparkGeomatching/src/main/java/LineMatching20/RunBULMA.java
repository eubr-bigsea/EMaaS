package LineMatching20;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

import LineDependencies.GeoLine;
import scala.Tuple2;

public class RunBULMA {

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Usage: <shape file> <GPS file> <directory of output path> <number of partitions>");
			System.exit(1);
		}

		Long tempoInicial = System.currentTimeMillis();
		
		String pathFileShapes = args[0];
		String pathGPSFile = args[1];
		String pathOutput = args[2];
		int minPartitions = Integer.valueOf(args[3]);
		
		
//		SparkConf sparkConf = new SparkConf().setAppName("GeoMatchingSpark").setMaster("local");
		SparkSession spark = SparkSession
				  .builder()
				  .master("local")
				  .config("spark.some.config.option", "some-value")
				  .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
				  .getOrCreate();
		
		Dataset<Tuple2<String, GeoLine>> lines = MatchingRoutes.generateDataFrames(pathFileShapes, pathGPSFile, minPartitions, spark);
		Dataset<String> output = MatchingRoutes.run(lines,minPartitions, spark);
		output.toJavaRDD().saveAsTextFile(pathOutput);
				
		System.out.println("Execution time with Dataset: " + (System.currentTimeMillis() - tempoInicial));
		
	}

}
