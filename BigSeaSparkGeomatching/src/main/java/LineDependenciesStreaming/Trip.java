package LineDependenciesStreaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import LineDependencies.ShapeLine;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;

public class Trip {
	
	private Integer route;
	private GPSPoint initialPoint;
	private Map<GPSPoint, Tuple2<ShapePoint, Float>> path; // MAP<POINT_GPS, TUPLE<POINT_SHAPE, DISTANCE>> PATH
	private GeoPoint endPoint;
	private List<GPSPoint> outliersBefore;
	private List<GPSPoint> outliersAfter;
	private ShapeLine shapeMatched;
	private float distanceToInitialPoint;
	private boolean isNearToEndPoint;
	
	public Trip(){
		this.distanceToInitialPoint = Float.MAX_VALUE;
		this.isNearToEndPoint = false;
		this.path = new HashMap<>();
		this.outliersBefore = new ArrayList<>();
		this.outliersAfter = new ArrayList<>();
	}

	public boolean isNearToEndPoint() {
		return isNearToEndPoint;
	}

	public void setNearToEndPoint(boolean isNearToEndPoint) {
		this.isNearToEndPoint = isNearToEndPoint;
	}

	public boolean hasFoundInitialPoint() {
		return this.initialPoint != null;
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

	public Map<GPSPoint, Tuple2<ShapePoint, Float>> getPath() {
		return path;
	}

	public void setPath(Map<GPSPoint, Tuple2<ShapePoint, Float>> path) {
		this.path = path;
	}

	public GeoPoint getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(GPSPoint endPoint) {
		this.endPoint = endPoint;
	}

	public void addOutlierBefore(GPSPoint gpsPoint) {
		if (gpsPoint != null && !outliersBefore.contains(gpsPoint))
			outliersBefore.add(gpsPoint);
	}
	
	public List<GPSPoint> getOutliersBefore() {
		return outliersBefore;
	}

	public void setOutliersBefore(List<GPSPoint> outliersBefore) {
		this.outliersBefore = outliersBefore;
	}

	public void addOutlierAfter(GPSPoint gpsPoint) {
		if (gpsPoint != null && !outliersAfter.contains(gpsPoint))
			outliersAfter.add(gpsPoint);
	}
	
	public List<GPSPoint> getOutliersAfter() {
		return outliersAfter;
	}

	public void setOutliersAfter(List<GPSPoint> outliersAfter) {
		this.outliersAfter = outliersAfter;
	}

	public ShapeLine getShapeMatched() {
		return shapeMatched;
	}

	public void setShapeMatched(ShapeLine shapeMatched) {
		if (shapeMatched != null)
			this.shapeMatched = shapeMatched;
	}

	@Override
	public String toString() {
		return "Trip [route=" + route + ", initalPoint=" + initialPoint + ", path=" + path + ", endPoint=" + endPoint
				+ ", outliersBefore=" + outliersBefore + ", outliersAfter=" + outliersAfter + ", shapeMatched="
				+ shapeMatched + "]";
	}

	public void addPointToPath(GPSPoint gpsPoint, ShapePoint closestPoint, Float smallerDistance) {
		this.path.put(gpsPoint, new Tuple2<ShapePoint, Float>(closestPoint, smallerDistance));
		this.endPoint = gpsPoint;
	}
}