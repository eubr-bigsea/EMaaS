package recordLinkage.dependencies;

import java.io.Serializable;

public class BulmaOutput implements Serializable, Comparable<BulmaOutput> {
	
	private static final long serialVersionUID = 3518929651995019131L;
	private String tripNum;
	private String route;
	private String shapeId;
	private String shapeSequence;
	private String latShape;
	private String lonShape;
	private String gpsPointId;
	private String busCode;
	private String timestamp;
	private String latGPS;
	private String lonGPS;
	private String dinstance;
	private String thresholdProblem;
	private String tripProblem;
	private String gps_datetime;
	
	public BulmaOutput() {
		super();
	}
		
	public BulmaOutput(String tripNum, String route, String shapeId, String shapeSequence, String latShape,
			String lonShape, String gpsPointId, String busCode, String timestamp, String latGPS, String lonGPS,
			String dinstance, String thresholdProblem, String tripProblem, String gps_date) {
		this.tripNum = tripNum;
		this.route = route;
		this.shapeId = shapeId;
		this.shapeSequence = shapeSequence;
		this.latShape = latShape;
		this.lonShape = lonShape;
		this.gpsPointId = gpsPointId;
		this.busCode = busCode;
		this.timestamp = timestamp;
		this.latGPS = latGPS;
		this.lonGPS = lonGPS;
		this.dinstance = dinstance;
		this.thresholdProblem = thresholdProblem;
		this.tripProblem = tripProblem;
		this.gps_datetime = gps_date + " " + timestamp;
	}

	public String getTripNum() {
		return tripNum;
	}

	public void setTripNum(String tripNum) {
		this.tripNum = tripNum;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public String getShapeId() {
		return shapeId;
	}

	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}

	public String getShapeSequence() {
		return shapeSequence;
	}

	public void setShapeSequence(String shapeSequence) {
		this.shapeSequence = shapeSequence;
	}

	public String getLatShape() {
		return latShape;
	}

	public void setLatShape(String latShape) {
		this.latShape = latShape;
	}

	public String getLonShape() {
		return lonShape;
	}

	public void setLonShape(String lonShape) {
		this.lonShape = lonShape;
	}

	public String getGpsPointId() {
		return gpsPointId;
	}

	public void setGpsPointId(String gpsPointId) {
		this.gpsPointId = gpsPointId;
	}

	public String getBusCode() {
		return busCode;
	}

	public void setBusCode(String busCode) {
		this.busCode = busCode;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getLatGPS() {
		return latGPS;
	}

	public void setLatGPS(String latGPS) {
		this.latGPS = latGPS;
	}

	public String getLonGPS() {
		return lonGPS;
	}

	public void setLonGPS(String lonGPS) {
		this.lonGPS = lonGPS;
	}

	public String getDinstance() {
		return dinstance;
	}

	public void setDinstance(String dinstance) {
		this.dinstance = dinstance;
	}

	public String getThresholdProblem() {
		return thresholdProblem;
	}

	public void setThresholdProblem(String thresholdProblem) {
		this.thresholdProblem = thresholdProblem;
	}

	public String getTripProblem() {
		return tripProblem;
	}

	public void setTripProblem(String tripProblem) {
		this.tripProblem = tripProblem;
	}
	
	public String getGps_datetime() {
		return gps_datetime;
	}

	public void setGps_datetime(String gps_datetime) {
		this.gps_datetime = gps_datetime;
	}
	
	@Override
	public String toString() {
		return tripNum + "," + route + "," + shapeId + ","
				+ shapeSequence + "," + latShape + "," + lonShape + "," + gpsPointId
				+ "," + busCode + "," + timestamp + "," + latGPS + "," + lonGPS
				+ "," + dinstance + "," + thresholdProblem + "," + tripProblem;
	}	
	
	@Override
	public int compareTo(BulmaOutput otherOutput) {
		if (Integer.parseInt(this.timestamp.replaceAll(":", "")) < Integer.parseInt(otherOutput.timestamp.replaceAll(":", ""))) {
            return -1;
        }
        if (Integer.parseInt(this.timestamp.replaceAll(":", "")) > Integer.parseInt(otherOutput.timestamp.replaceAll(":", ""))) {
            return 1;
        }
        return 0;
	}
	
}
