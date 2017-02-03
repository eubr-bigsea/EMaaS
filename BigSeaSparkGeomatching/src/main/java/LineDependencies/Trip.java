package LineDependencies;

import java.io.Serializable;
import java.util.List;

import PointDependencies.GeoPoint;

public class Trip implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private ShapeLine shapeLine;
	private List<GeoPoint> gpsPoints;
	
	public Trip(ShapeLine shapeLine, List<GeoPoint> pointsTripGPS) throws Exception {
		if(shapeLine == null  || pointsTripGPS == null || pointsTripGPS.isEmpty()) {
			throw new Exception("Parameter value invalid!");
		}
		this.shapeLine = shapeLine;
		this.gpsPoints = pointsTripGPS;
	}

	public ShapeLine getShapeLine() {
		return this.shapeLine;
	}
	
	public List<GeoPoint> getShapePoints() {
		return this.shapeLine.getListGeoPoints();
	}

	public void setShapeLine(ShapeLine shapePoints) {
		this.shapeLine = shapePoints;
	}

	public List<GeoPoint> getGpsPoints() {
		return this.gpsPoints;
	}

	public void setGpsPoints(List<GeoPoint> gpsPoints) {
		this.gpsPoints = gpsPoints;
	}
}
