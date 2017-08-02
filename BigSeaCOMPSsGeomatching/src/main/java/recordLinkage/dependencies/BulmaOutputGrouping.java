package recordLinkage.dependencies;

import java.io.Serializable;
import java.util.Map;

public class BulmaOutputGrouping implements Serializable {

	private static final long serialVersionUID = 1L;
	private Map<String, BulmaOutput> mapOutputGrouping;

	public BulmaOutputGrouping(Map<String, BulmaOutput> mapOutputGrouping) {
		this.mapOutputGrouping = mapOutputGrouping;
	}

	public Map<String, BulmaOutput> getMapOutputGrouping() {
		return mapOutputGrouping;
	}

	public void setMapOutputGrouping(Map<String, BulmaOutput> mapOutputGrouping) {
		this.mapOutputGrouping = mapOutputGrouping;
	}

	public boolean containsShapeSequence(String shapeSequence) {
		return mapOutputGrouping.containsKey(shapeSequence);
	}
	
}
