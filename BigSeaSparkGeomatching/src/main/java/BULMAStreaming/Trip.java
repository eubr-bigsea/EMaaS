package BULMAStreaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import BULMADependences.ShapeLine;
import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import PointDependencies.ShapePoint;
import scala.Tuple2;

public class Trip {

	private Integer route;
	private GPSPoint initialPoint;
	private Map<GPSPoint, Tuple2<ShapePoint, Float>> path;
	private GeoPoint endPoint;
	private ShapeLine shapeMatched;
	private float distanceToInitialPoint;
	private List<GPSPoint> outliersInSequence;
	
	public Trip(){
		this.distanceToInitialPoint = Float.MAX_VALUE;
		this.path = new HashMap<GPSPoint, Tuple2<ShapePoint, Float>>();
		this.outliersInSequence = new ArrayList<GPSPoint>();
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
	
	public ShapeLine getShapeMatched() {
		return shapeMatched;
	}

	public void setShapeMatched(ShapeLine shapeMatched) {
		if (shapeMatched != null)
			this.shapeMatched = shapeMatched;
	}

	public int getNumberOfOutliersInSequence() {
		return this.outliersInSequence.size();
	}

	public void cleanOutliersInSequenceList() {
		this.outliersInSequence = new ArrayList<GPSPoint>();
	}
	
	public void addOutlierInSequence(GPSPoint outlier) {
		this.outliersInSequence.add(outlier);
	}

	public List<GPSPoint> getOutliersInSequence() {
		return outliersInSequence;
	}

	public void setOutliersInSequence(List<GPSPoint> outliersInSequence) {
		this.outliersInSequence = outliersInSequence;
	}

	@Override
	public String toString() {
		return "Trip [route=" + route + ", initalPoint=" + initialPoint + ", path=" + path + ", endPoint=" + endPoint
				  + ", shapeMatched=" + shapeMatched + "]";
	}

	public void addPointToPath(GPSPoint gpsPoint, ShapePoint closestPoint, Float smallerDistance) {
		this.path.put(gpsPoint, new Tuple2<ShapePoint, Float>(closestPoint, smallerDistance));
		this.endPoint = gpsPoint;
	}
}