package LineDependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;

import PointDependencies.GeoPoint;
import PolygonDependencies.InputTypes;

import java.io.Serializable;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import PolygonDependencies.InputTypes;


public class GeoLine extends GeoObject implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String id;
	private LineString line;
	private String blockingKey;
	private List<GeoPoint> listGeoPoints;
	private float greaterDistancePoints;

	//Used in polygons matching
	public GeoLine(String geometryGIS, String geoName, InputTypes type, Integer idGeometry, Integer idInDataset) throws ParseException {
		super(geometryGIS, type, idGeometry, geoName, idInDataset);
	}
	
	public GeoLine(String id, LineString line, String blockingKey) {
		this.id = id;
		this.line = line;
		this.blockingKey = blockingKey;
		this.listGeoPoints = new ArrayList<GeoPoint>();
	}	
	
	public GeoLine(String id, LineString line, String blockingKey, List<GeoPoint> listGeoPoints, float greaterDistancePoints) {
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

	public List<GeoPoint> getListGeoPoints() {
		return this.listGeoPoints;
	}

	public void setListGeoPoints(List<GeoPoint> listGeoPoints) {
		this.listGeoPoints = listGeoPoints;
	}
	
	public float getGreaterDistancePoints() {
		return greaterDistancePoints;
	}

	public void setGreaterDistancePoints(float greaterDistancePoints) {
		this.greaterDistancePoints = greaterDistancePoints;
	}
	
	@Override
	public Geometry getGeometry() {
		return geometry;
	}
	@Override
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	@Override
	public Integer getIdGeometry() {
		return idGeometry;
	}

	@Override
	public void setIdGeometry(Integer idGeometry) {
		this.idGeometry = idGeometry;
	}

	@Override
	public String getGeoName() {
		return geoName;
	}

	@Override
	public void setGeoName(String geoName) {
		this.geoName = geoName;
	}
	@Override
	public InputTypes getType() {
		return type;
	}
	@Override
	public void setType(InputTypes type) {
		this.type = type;
	}

	@Override
	public Integer getIdInDataset() {
		return idInDataset;
	}

	@Override
	public void setIdInDataset(Integer idInDataset) {
		this.idInDataset = idInDataset;
	}

	@Override
	public String toString() {
		return "GeoLine [id=" + id + ", line=" + line + ", blockingKey=" + blockingKey + ", listGeoPoints="
				+ listGeoPoints + "]";
	}
}