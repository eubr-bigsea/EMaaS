package approaches_streaming.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import approaches_streaming.utils.StringSimilarity;
import scala.Tuple2;
import scala.Tuple3;

public class BasicStreaming {

	public static final Double THRESHOLD = 0.7;

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 5) {
			System.err.println("Usage: <hostname> <port> <batchDuration> <windowDuration> <slideDuration>");
			System.exit(1);
		}

		String hostname = args[0];
		Integer port = Integer.valueOf(args[1]);
		Integer batchDuration = Integer.valueOf(args[2]); //The time interval at which streaming data will be divided into batches
		Integer windowDuration = Integer.valueOf(args[3]); //The time interval to read datas and clean
		Integer slideDuration = Integer.valueOf(args[4]); //The time interval to execute all batches
		
		//3 threads working
		SparkConf sparkConf = new SparkConf().setMaster("local[3]").setAppName("BasicStreaming");
		JavaStreamingContext context = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
		
		JavaDStream<String> lines = context.socketTextStream(hostname, port).
				window(Durations.seconds(windowDuration), Durations.seconds(slideDuration));
	
		JavaDStream<String> words = lines.flatMap(new FlatMapFunction<String, String>() {

			public Iterator<String> call(String entry) throws Exception {
				return Arrays.asList(entry.split(" ")).iterator();
			}
		});
		
		JavaPairDStream<String, String> key_word_pair = words.mapToPair(new PairFunction<String, String, String>() {

			public Tuple2<String, String> call(String entry) throws Exception {
				return new Tuple2<String, String>(entry.toLowerCase().substring(0, 1), entry);
			}
		});
		
		JavaPairDStream<String, Iterable<String>> group_by_key = key_word_pair.groupByKey();

		JavaDStream<Tuple3<String, String, Double>> similarity = group_by_key
				.flatMap(new FlatMapFunction<Tuple2<String, Iterable<String>>, Tuple3<String, String, Double>>() {

					public Iterator<Tuple3<String, String, Double>> call(Tuple2<String, Iterable<String>> entry)
							throws Exception {

						List<Tuple3<String, String, Double>> list = new ArrayList<Tuple3<String, String, Double>>();

						Iterable<String> words = entry._2;

						int count = 0;
						for (Iterator<String> iterator = words.iterator(); iterator.hasNext();) {
							String w1 = (String) iterator.next();
							count++;
							int it = 0;
							for (Iterator<String> iterator2 = words.iterator(); iterator2.hasNext();) {
								for (int i = it; i < count; i++) {
									it++;
									iterator2.next();
								}
								if (iterator2.hasNext()) {
									String w2 = (String) iterator2.next();
									Double similarity = StringSimilarity.similarity(w1, w2);
//									if (similarity >= THRESHOLD) {
										list.add(new Tuple3<String, String, Double>(w1, w2, similarity));
//									}
								}
							}
						}
						return list.iterator();
					}
				}); 
		
		//TODO salve as DataFrame
//		
		similarity.dstream().saveAsTextFiles("output/similarity", "");

		context.start();
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
}