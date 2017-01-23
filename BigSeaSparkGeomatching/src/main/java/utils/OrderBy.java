package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import scala.Tuple2;
import scala.Tuple3;

public class OrderBy {
	
	public OrderBy(){}
	
	public static LinkedHashMap<Tuple2<Long, Long>, Tuple2<Long, Long>> orderByComparisonsDescending(
			Map<Tuple2<Long, Long>, Tuple2<Long, Long>> passedMap) {

		List<Tuple2<Long, Long>> mapKeys = new ArrayList<>(passedMap.keySet());
		List<Tuple2<Long, Long>> mapValues = new ArrayList<>(passedMap.values());
		
		List<Long> values = new ArrayList<>();
		
		for (Tuple2<Long, Long> tuple: mapValues) {
			values.add(tuple._1);
		}
		
		Collections.sort(values, Collections.reverseOrder());

		LinkedHashMap<Tuple2<Long, Long>, Tuple2<Long, Long>> sortedMap = new LinkedHashMap<>();

		Iterator<Long> valueIt = values.iterator();
		while (valueIt.hasNext()) {
			Long val = valueIt.next();
			Iterator<Tuple2<Long, Long>> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Tuple2<Long, Long> key = keyIt.next();
				Long comp1 = passedMap.get(key)._1;
				Long comp2 = val;

				if (comp1.equals(comp2)) {
					keyIt.remove();
					sortedMap.put(key, new Tuple2<Long, Long>(val, passedMap.get(key)._2 ));
					break;
				}
			}
		}
		return sortedMap;
	}	
	
	public static LinkedHashMap<Tuple3<Long, Long, Long>, Long> orderByValueDescending(
			Map<Tuple3<Long, Long, Long>, Long> passedMap) {

		List<Tuple3<Long, Long, Long>> mapKeys = new ArrayList<>(passedMap.keySet());
		List<Long> mapValues = new ArrayList<>(passedMap.values());
		Collections.sort(mapValues, Collections.reverseOrder());

		LinkedHashMap<Tuple3<Long, Long, Long>, Long> sortedMap = new LinkedHashMap<>();

		Iterator<Long> valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Long val = valueIt.next();
			Iterator<Tuple3<Long, Long, Long>> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Tuple3<Long, Long, Long> key = keyIt.next();
				Long comp1 = passedMap.get(key);
				Long comp2 = val;

				if (comp1.equals(comp2)) {
					keyIt.remove();
					sortedMap.put(key, val);
					break;
				}
			}
		}
		return sortedMap;
	}		
	
}
