package approaches_streaming.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import approaches_streaming.utils.MatchingObject;
import approaches_streaming.utils.StringSimilarity;
import scala.Tuple2;
import scala.Tuple3;

public class IncrementalBasicStreaming {

	private static final Double THRESHOLD = 0.7;
	// key , entity
	private static Map<String, List<String>> mapBlockDataStructure = new HashMap<>();
	// entity, similarity
	private static Map<String, MatchingObject> mapSimilarityDataStructure = new HashMap<>();
	private static Long id = 0L;

	public static Long getNextId() {
		return id++;
	}

	@SuppressWarnings({ "serial", "resource" })
	public static void main(String[] args) {

		if (args.length < 3) {
			System.err.println("Usage: <hostname> <port> <batchDuration>");
			System.exit(1);
		}

		String hostname = args[0];
		Integer port = Integer.valueOf(args[1]);
		Integer batchDuration = Integer.valueOf(args[2]);
		
		// 3 threads working
		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("BasicStreaming");
		JavaStreamingContext context = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
	

		JavaDStream<String> lines = context.socketTextStream(hostname, port);
		JavaDStream<String> words = lines.flatMap(new FlatMapFunction<String, String>() {

			public Iterator<String> call(String entry) throws Exception {
				return Arrays.asList(entry.split(" ")).iterator();
			}
		});

		JavaPairDStream<String, Tuple2<String,Long>> key_word_pair  = words.mapToPair(new PairFunction<String, String, Tuple2<String,Long>>() {

			public Tuple2<String, Tuple2<String, Long>> call(String entry) throws Exception {
				String entryKey = getKey(entry);
				
				if (!mapBlockDataStructure.containsKey(entryKey)) {
					List<String> listEntities = new ArrayList<>();					
					mapBlockDataStructure.put(entryKey, listEntities);
				} 				
				List<String> listEntities = mapBlockDataStructure.get(entryKey);
				listEntities.add(entry);				
				return new Tuple2<String, Tuple2<String,Long>>(entryKey, new Tuple2<String,Long>(entry, getNextId()));
			}

			private String getKey(String entry) {				
				return entry.toLowerCase().substring(0, 1);
			}
		});
		
		JavaDStream<List<Tuple3<String, String, Double>>> rddOutput = key_word_pair.map(new Function<Tuple2<String, Tuple2<String,Long>>, List<Tuple3<String, String, Double>>>() {

			@Override
			public List<Tuple3<String, String, Double>> call(Tuple2<String, Tuple2<String,Long>> entry) throws Exception {
				
				updateMapSimilarityDataStructure(entry._1);
				
				List<Tuple3<String, String, Double>> listOutput = new ArrayList<>();
				String entity = entry._2._1;				
				MatchingObject matchingObject = mapSimilarityDataStructure.get(entity);
								
				if (matchingObject != null && matchingObject.getMapValuesMatched() != null) {
					for (Entry<String, Double> object : matchingObject.getMapValuesMatched().entrySet()) {
						listOutput.add(new Tuple3<String, String, Double>(entity, object.getKey(), object.getValue()));
					}
				}
				return listOutput;
			}
			
			private void updateMapSimilarityDataStructure(String key) {
				List<String> listEntities = mapBlockDataStructure.get(key);
				
				for (int indexCurrentKey = 0; indexCurrentKey < listEntities.size(); indexCurrentKey++) {
					if (!mapSimilarityDataStructure.containsKey(indexCurrentKey)) {
						MatchingObject object = new MatchingObject();
						object.addId(getNextId());
					}
					String currentKey = listEntities.get(indexCurrentKey);

					if (!mapSimilarityDataStructure.containsKey(currentKey)) {
						mapSimilarityDataStructure.put(currentKey, new MatchingObject());
					}				
					MatchingObject currentObject = mapSimilarityDataStructure.get(currentKey);
					for (int indexCurrentValue = 0; indexCurrentValue < listEntities.size(); indexCurrentValue++ ) {
						if (indexCurrentKey != indexCurrentValue) {
							
							String currentValue = listEntities.get(indexCurrentValue);
							if (currentValue != null && currentKey != null) {
								Double similarity = StringSimilarity.similarity(currentKey, currentValue);
								currentObject.addValueMatched(currentValue, similarity);
							}
						}
					}
				}
			}
		});
		
		rddOutput.dstream().saveAsTextFiles("output/similarity", "");
		context.start();
				
		try {
			context.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
