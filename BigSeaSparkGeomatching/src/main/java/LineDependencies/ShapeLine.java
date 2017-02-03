package LineDependencies;

import java.util.List;

import com.vividsolutions.jts.geom.LineString;

import PointDependencies.GeoPoint;

public class ShapeLine extends GeoLine {

	private static final long serialVersionUID = 1L;
	
	private String distanceTraveled;
	private String route;
	private int thresholdDistance;
		
	public ShapeLine(String id, LineString lineString, String distanceTraveled, String blockingKey) {
		super(id, lineString, blockingKey);
		this.distanceTraveled = distanceTraveled;
	}
	
	public ShapeLine(String id, LineString lineString, String distanceTraveled, String blockingKey,
			List<GeoPoint> listGeoPoints, String route) {
		super(id, lineString, blockingKey, listGeoPoints);
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

	public void setDistanceTraveled(String distanceTraveled) {
		this.distanceTraveled = distanceTraveled;
	}
	
	public int getThresholdDistance() {
		return this.thresholdDistance;
	}

	public void setThresholdDistance(int thresholdDistance) {
		this.thresholdDistance = thresholdDistance;
	}

	@Override
	public String toString() {
		return "ShapeLine [distanceTraveled=" + distanceTraveled + ", route=" + route + ", thresholdDistance="
				+ thresholdDistance + "]";
	}
}
