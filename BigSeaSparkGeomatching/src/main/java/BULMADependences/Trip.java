package BULMADependences;

import java.io.Serializable;
import java.util.List;

import PointDependencies.GeoPoint;

public class Trip implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private ShapeLine shapeLine;
	private List<GeoPoint> gpsPoints;
	private Integer firstIndex;
	private Integer lastIndex;
	private Problem problem;
	
	public Trip(ShapeLine shapeLine, List<GeoPoint> pointsTripGPS, Problem problem) throws Exception {
		if(pointsTripGPS == null || pointsTripGPS.isEmpty()) {
			throw new Exception("Parameter value invalid!");
		}
		this.shapeLine = shapeLine;
		this.gpsPoints = pointsTripGPS;
		this.problem = problem;
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

	public Integer getLastIndex() {
		return lastIndex;
	}

	public void setLastIndex(Integer lastIndex) {
		this.lastIndex = lastIndex;
	}

	public Integer getFirstIndex() {
		return firstIndex;
	}

	public void setFirstIndex(Integer firstIndex) {
		this.firstIndex = firstIndex;
	}

	public List<GeoPoint> getGpsPoints() {
		return gpsPoints;
	}

	public List<GeoPoint> getGPSPoints() {
		return this.gpsPoints;
	}

	public void setGpsPoints(List<GeoPoint> gpsPoints) {
		this.gpsPoints = gpsPoints;
	}
			
	public Problem getProblem() {
		return problem;
	}

	@Override
	public String toString() {
		return "Trip [shapeLine=" + shapeLine + ", gpsPoints=" + gpsPoints + ", Problem=" + problem.getCode() + "]";
	}
}