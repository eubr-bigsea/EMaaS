package IncrementalLineDependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import LineDependencies.ShapeLine;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class Trip {
	
	private Integer route;
	private GeoPoint initialPoint;
	private Map<GeoPoint, Tuple2<GeoPoint, Double>> path; // MAP<POINTS_GPS, TUPLE<POINT_SHAPE, DISTANCE>> PATH
	private GeoPoint endPoint;
	private List<GeoPoint> outliersBefore; // somente pontos quando sair da garagem, antes da primeira trip
	private List<GeoPoint> outliersAfter;
	private ShapeLine shapeMatching;
	
	public Trip(){
		this.path = new HashMap<>();
		this.outliersBefore = new ArrayList<>();
		this.outliersAfter = new ArrayList<>();
	}

	public Integer getRoute() {
		return route;
	}

	public void setRoute(Integer route) {
		this.route = route;
	}

	public GeoPoint getInitialPoint() {
		return initialPoint;
	}

	public void setInitialPoint(GeoPoint initialPoint) {
		this.initialPoint = initialPoint;
	}

	public Map<GeoPoint, Tuple2<GeoPoint, Double>> getPath() {
		return path;
	}

	public void setPath(Map<GeoPoint, Tuple2<GeoPoint, Double>> path) {
		this.path = path;
	}

	public GeoPoint getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(GeoPoint endPoint) {
		this.endPoint = endPoint;
	}

	public List<GeoPoint> getOutliersBefore() {
		return outliersBefore;
	}

	public void setOutliersBefore(List<GeoPoint> outliersBefore) {
		this.outliersBefore = outliersBefore;
	}

	public List<GeoPoint> getOutliersAfter() {
		return outliersAfter;
	}

	public void setOutliersAfter(List<GeoPoint> outliersAfter) {
		this.outliersAfter = outliersAfter;
	}

	public ShapeLine getShapeMatching() {
		return shapeMatching;
	}

	public void setShapeMatching(ShapeLine shapeMatching) {
		this.shapeMatching = shapeMatching;
	}

	@Override
	public String toString() {
		return "Trip [route=" + route + ", initalPoint=" + initialPoint + ", path=" + path + ", endPoint=" + endPoint
				+ ", outliersBefore=" + outliersBefore + ", outliersAfter=" + outliersAfter + ", shapeMatching="
				+ shapeMatching + "]";
	}
}