package LineMatching20;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import LineDependencies.GeoLine;
import LineDependencies.GeoObject;
import scala.Tuple2;

public class RunBULMAv2 {

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Usage: <shape file> <GPS file> <directory of output path> <number of partitions>");
			System.exit(1);
		}
		Map<String, String> env = System.getenv();

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
		
		Function2<Integer, Iterator<String>, Iterator<String>> removeHeader = new Function2<Integer, Iterator<String>, Iterator<String>>() {
			@Override
			public Iterator<String> call(Integer index, Iterator<String> iterator) throws Exception {
				if (index == 0 && iterator.hasNext()) {
					iterator.next();
					return iterator;
				} else {
					return iterator;
				}
			}
		};

		JavaSparkContext ctx = new JavaSparkContext(spark.sparkContext());

		Dataset<Row> datasetGPSFile = spark.read().text(pathGPSFile);
		Dataset<Row> datasetShapeFile = spark.read().text(pathFileShapes);
		
		Dataset<Tuple2<String, GeoLine>> lines = MatchingRoutesV2.generateDataFrames(datasetShapeFile, datasetGPSFile, minPartitions, spark);
		Dataset<String> output = MatchingRoutesV2.run(lines,minPartitions, spark);
		output.toJavaRDD().saveAsTextFile(pathOutput);
		
		System.out.println("Execution time with Dataset: " + (System.currentTimeMillis() - tempoInicial));
		
	}

}
