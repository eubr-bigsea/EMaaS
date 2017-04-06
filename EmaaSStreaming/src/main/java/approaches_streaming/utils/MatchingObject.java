package approaches_streaming.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.exception.NullArgumentException;

public class MatchingObject {
	
	private List<Long> listId;
	private Map<String, Double> mapValuesMatched;
	
	public MatchingObject() {
		super();
		this.listId = new ArrayList<>();
		this.mapValuesMatched = new HashMap<>();
	}
	
	public List<Long> getListId() {
		return listId;
	}

	public void setListId(List<Long> listId) {
		this.listId = listId;
	}

	public Map<String, Double> getMapValuesMatched() {
		return mapValuesMatched;
	}

	public void setMapValuesMatched(Map<String, Double> mapValuesMatched) {
		this.mapValuesMatched = mapValuesMatched;
	}
	
	public void addId (Long id){
		if (id == null) {
			throw new NullArgumentException();
		}
		this.listId.add(id);
	}
	
	public void addValueMatched(String entity, Double similarity) {
		if (entity == null || similarity == null) {
			throw new NullArgumentException();
		}
		this.mapValuesMatched.put(entity, similarity);
	}
	
	@Override
	public String toString() {
		String output = "";
		for (Entry<String, Double> entry: mapValuesMatched.entrySet()) {
			output+= entry.getKey() +"=" +entry.getValue();
		}
		return output;
	}
}
