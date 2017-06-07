package BULMADependences;

public class BULMAInputShape {
	
	String route;
	String shapeId;
	String latitude;
	String longitude;
	String sequence;
	String distanceTraveled;

	public BULMAInputShape(String params) {
		// TODO split params to set attributes 
	}

	public BULMAInputShape(String route, String shapeId, String latitude, String longitude, String sequence, String distanceTraveled) {
		this.route = route;
		this.shapeId = shapeId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.sequence = sequence;
		this.distanceTraveled = distanceTraveled;		
	}
	
	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getDistanceTraveled() {
		return distanceTraveled;
	}

	public void setDistanceTraveled(String distanceTraveled) {
		this.distanceTraveled = distanceTraveled;
	}

	public String getShapeId() {
		return shapeId;
	}

	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}

}
