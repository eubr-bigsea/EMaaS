package LineDependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.LineString;

import PointDependencies.GeoPoint;

public abstract class GeoLine implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String id;
	private LineString line;
	private String blockingKey;
	private List<GeoPoint> listGeoPoints;
		
	public GeoLine(String id, LineString line, String blockingKey) {
		this.id = id;
		this.line = line;
		this.blockingKey = blockingKey;
		this.listGeoPoints = new ArrayList<GeoPoint>();
	}	
	
	public GeoLine(String id, LineString line, String blockingKey, List<GeoPoint> listGeoPoints) {
		this(id, line, blockingKey);
		this.listGeoPoints = listGeoPoints;
	}
	
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}	
	
	public LineString getLine() {
		return this.line;
	}

	public void setLine(LineString line) {
		this.line = line;
	}

	public String getBlockingKey() {
		return this.blockingKey;
	}

	public void setBlockingKey(String blockingKey) {
		this.blockingKey = blockingKey;
	}

	public List<GeoPoint> getListGeoPoints() {
		return this.listGeoPoints;
	}

	public void setListGeoPoints(List<GeoPoint> listGeoPoints) {
		this.listGeoPoints = listGeoPoints;
	}

	@Override
	public String toString() {
		return "GeoLine [id=" + id + ", line=" + line + ", blockingKey=" + blockingKey + ", listGeoPoints="
				+ listGeoPoints + "]";
	}
}