package IncrementalLineMatching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import IncrementalLineDependencies.Trip;
import LineDependencies.ShapeLine;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class IncrementalBULMA {
	
	// MAP<ROUTE, LIST<TUPLE2<INITIAL_POINT, SHAPE>>
	private static Map<String, List<Tuple2<GeoPoint, ShapeLine>>> mapRouteShapeLine;
	// MAP<BUS_CODE, TRIP>
	private static Map<String, Trip> mapBusCodeTrip;
	
	public IncrementalBULMA(){
		mapRouteShapeLine = new HashMap<>();
		mapBusCodeTrip = new HashMap<>();
	}
	
	public static void main(String[] args) {
		if (args.length < 6) {
			System.err.println("Usage: <shape file path> <GPS files hostname> <GPS files port> <output path> "
					+ "<partitions number> <batch duration");
			System.exit(1);
		}
		
		String pathFileShapes = args[0];
		String hostnameGPSFile = args[1];
		Integer hostnameGPSPort = Integer.valueOf(args[2]);
		String pathOutput = args[3];
		int minPartitions = Integer.valueOf(args[4]);
		int batchDuration = Integer.valueOf(args[5]);

		if (pathFileShapes.equals(pathOutput)) {
			System.out.println("The output directory should not be the same as the Shape file directory.");
		}

		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("IncrementalBulma");
		JavaStreamingContext context = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));

		generateOutputFiles(pathFileShapes, hostnameGPSFile, hostnameGPSPort, pathOutput, minPartitions, context);
		
		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void generateOutputFiles(String pathFileShapes, String hostnameGPSFile, Integer hostnameGPSPort, String pathOutput,
			int minPartitions, JavaStreamingContext context) {

		JavaDStream<String> rddOutputBuLMA = executeBULMA(pathFileShapes, hostnameGPSFile, hostnameGPSPort, minPartitions, context);
		rddOutputBuLMA.dstream().saveAsTextFiles(pathOutput, "");
	}
	
	@SuppressWarnings("serial")
	private static JavaDStream<String> executeBULMA(String pathFileShapes, String hostnameGPSFile, Integer hostnameGPSPort, int minPartitions, JavaStreamingContext ctx) {
		
		JavaDStream<String> gpsString = ctx.socketTextStream(hostnameGPSFile, hostnameGPSPort);
		JavaDStream<String> shapeString = ctx.textFileStream(pathFileShapes);
		
		return null;
	}
}