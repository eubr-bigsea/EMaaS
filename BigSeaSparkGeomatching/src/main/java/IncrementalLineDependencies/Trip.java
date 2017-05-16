package IncrementalLineDependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Point;

import LineDependencies.ShapeLine;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class Trip {
	
	private Integer route;
	private GPSPoint initialPoint;
	private Map<GeoPoint, Tuple2<Point, Float>> path; // MAP<POINT_GPS, TUPLE<POINT_SHAPE, DISTANCE>> PATH
	private GeoPoint endPoint;
	private List<GPSPoint> outliersBefore;
	private List<GPSPoint> outliersAfter;
	private ShapeLine shapeMatching;
	private float distanceToInitialPoint;
	private boolean hasFoundInitialPoint;
	
	public Trip(){
		this.hasFoundInitialPoint = false;
		this.path = new HashMap<>();
		this.outliersBefore = new ArrayList<>();
		this.outliersAfter = new ArrayList<>();
	}

	public boolean hasFoundInitialPoint() {
		return hasFoundInitialPoint;
	}

	public void setHasFoundInitialPoint(boolean hasFoundInitialPoint) {
		this.hasFoundInitialPoint = hasFoundInitialPoint;
	}

	public float getDistanceToInitialPoint() {
		return distanceToInitialPoint;
	}

	public void setDistanceToInitialPoint(float distanceToInitialPoint) {
		this.distanceToInitialPoint = distanceToInitialPoint;
	}

	public Integer getRoute() {
		return route;
	}

	public void setRoute(Integer route) {
		this.route = route;
	}

	public GPSPoint getInitialPoint() {
		return initialPoint;
	}

	public void setInitialPoint(GPSPoint initialPoint) {
		if (initialPoint != null)
			this.initialPoint = initialPoint;
	}

	public Map<GeoPoint, Tuple2<Point, Float>> getPath() {
		return path;
	}

	public void setPath(Map<GeoPoint, Tuple2<Point, Float>> path) {
		this.path = path;
	}

	public GeoPoint getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(GPSPoint endPoint) {
		this.endPoint = endPoint;
	}

	public void addOutlierBefore(GPSPoint gpsPoint) {
		if (gpsPoint != null)
			outliersBefore.add(gpsPoint);
	}
	
	public List<GPSPoint> getOutliersBefore() {
		return outliersBefore;
	}

	public void setOutliersBefore(List<GPSPoint> outliersBefore) {
		this.outliersBefore = outliersBefore;
	}

	public void addOutlierAfter(GPSPoint gpsPoint) {
		if (gpsPoint != null)
			outliersAfter.add(gpsPoint);
	}
	
	public List<GPSPoint> getOutliersAfter() {
		return outliersAfter;
	}

	public void setOutliersAfter(List<GPSPoint> outliersAfter) {
		this.outliersAfter = outliersAfter;
	}

	public ShapeLine getShapeMatching() {
		return shapeMatching;
	}

	public void setShapeMatching(ShapeLine shapeMatching) {
		if (shapeMatching != null)
			this.shapeMatching = shapeMatching;
	}

	@Override
	public String toString() {
		return "Trip [route=" + route + ", initalPoint=" + initialPoint + ", path=" + path + ", endPoint=" + endPoint
				+ ", outliersBefore=" + outliersBefore + ", outliersAfter=" + outliersAfter + ", shapeMatching="
				+ shapeMatching + "]";
	}
}