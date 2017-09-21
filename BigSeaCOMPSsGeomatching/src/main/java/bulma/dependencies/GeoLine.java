package bulma.dependencies;

import java.io.Serializable;
import java.util.LinkedList;

import com.vividsolutions.jts.geom.LineString;


public class GeoLine implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String id;
	private LineString line;
	private String blockingKey;
	private LinkedList<GeoPoint> listGeoPoints;
	private float greaterDistancePoints;

	public GeoLine() {
		super();
	}
	
	public GeoLine(String id, LineString line, String blockingKey) {
		this.id = id;
		this.line = line;
		this.blockingKey = blockingKey;
		this.listGeoPoints = new LinkedList<GeoPoint>();
	}	
	
	public GeoLine(String id, LineString line, String blockingKey, LinkedList<GeoPoint> listGeoPoints, float greaterDistancePoints) {
		this(id, line, blockingKey);
		this.listGeoPoints = listGeoPoints;
		this.greaterDistancePoints = greaterDistancePoints;
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

	public LinkedList<GeoPoint> getListGeoPoints() {
		return this.listGeoPoints;
	}
	
	public GeoPoint getFirstPoint() {
		if (listGeoPoints.size() == 0) {
			return null;
		}
		return this.listGeoPoints.get(0);
	}
	
	public GeoPoint getLastPoint() {
		if (listGeoPoints.size() == 0) {
			return null;
		}
		return this.listGeoPoints.get(listGeoPoints.size() -1);
	}

	public void setListGeoPoints(LinkedList<GeoPoint> listGeoPoints) {
		this.listGeoPoints = listGeoPoints;
	}
	
	public float getGreaterDistancePoints() {
		return greaterDistancePoints;
	}

	public void setGreaterDistancePoints(float greaterDistancePoints) {
		this.greaterDistancePoints = greaterDistancePoints;
	}
	
	@Override
	public String toString() {
		return "GeoLine [id=" + id + ", line=" + line + ", blockingKey=" + blockingKey + ", listGeoPoints="
				+ listGeoPoints + "]";
	}
}
