package streaming;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class IncrementalBULMA {
	
	public static void main(String[] args) {

		if (args.length < 3) {
			System.err.println("Usage: <hostname> <port> <batchDuration>");
			System.exit(1);
		}

		String hostname = args[0];
		Integer port = Integer.valueOf(args[1]);
		Integer batchDuration = Integer.valueOf(args[2]);
		
		// 3 threads working
		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("Incremental BULMA");
		JavaStreamingContext context = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
	

		JavaDStream<String> points = context.socketTextStream(hostname, port);
		
		JavaPairDStream<String, Iterable<GeoPoint>> rddGPSPointsPair = points
				.mapToPair(new PairFunction<String, String, GeoPoint>() {

					@Override
					public Tuple2<String, GeoPoint> call(String s) throws Exception {
						GPSPoint gpsPoint = GPSPoint.createGPSPointWithId(s);
						return new Tuple2<String, GeoPoint>(gpsPoint.getBusCode(), gpsPoint);

					}
				}).groupByKey();
		
		rddGPSPointsPair.dstream().saveAsTextFiles("output/outputStreaming", "");

		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}