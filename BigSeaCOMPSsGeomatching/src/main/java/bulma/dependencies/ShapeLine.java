package bulma.dependencies;

import java.util.LinkedList;

import com.vividsolutions.jts.geom.LineString;


public class ShapeLine extends GeoLine {

	private static final long serialVersionUID = 1L;
	
	private Float distanceTraveled;
	private String route;
	private int thresholdDistance;
		
		
	public ShapeLine(String id, LineString lineString, Float distanceTraveled, String blockingKey,
			LinkedList<GeoPoint> listGeoPoints, String route, float greaterDistancePoints) {
		super(id, lineString, blockingKey, listGeoPoints, greaterDistancePoints);
		this.distanceTraveled = distanceTraveled;
		this.route = route;
	}
	
	public String getRoute() {
		return this.route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public Double getDistanceTraveled() {
		return Double.valueOf(distanceTraveled);
	}

	public void setDistanceTraveled(Float distanceTraveled) {
		this.distanceTraveled = distanceTraveled;
	}
	
	public int getThresholdDistance() {
		return this.thresholdDistance;
	}

	public void setThresholdDistance(int thresholdDistance) {
		this.thresholdDistance = thresholdDistance;
	}

//	@Override
//	public String toString() {
//		return "ShapeLine [distanceTraveled=" + distanceTraveled + ", route=" + route + ", thresholdDistance="
//				+ thresholdDistance + "]";
//	}
	
	@Override
	public String toString() {
		return getListGeoPoints().toString();
	}
}