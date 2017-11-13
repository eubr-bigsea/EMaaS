package recordLinkageDF.dependencies;

import java.io.IOException;

//arrival_time,departure_time,stop_id,stop_sequence,lat_stop,lng_stop,route_id,shape_id,closest_shape_point

public class BUSTEFieldsInputBusStop {

	private String arrivalTime;
	private String departureTime;
	private String stopId;
	private String stopSequence;
	private String lat;
	private String lng;
	private String routeId;
	private String shapeId;
	private String closestShapePoint;
	private static final int NUMBER_ATTRIBUTES = 9;

	public BUSTEFieldsInputBusStop(String params) throws IOException {
		String[] attributesArray = params.split(",");

		if (attributesArray.length != NUMBER_ATTRIBUTES) {
			throw new IOException("The Bus Stop input file should have " + NUMBER_ATTRIBUTES
					+ " attributes not null. The fields should be separated by comma.");
		}

		initializeAttributes(attributesArray[0], attributesArray[1], attributesArray[2], attributesArray[3],
				attributesArray[4], attributesArray[5], attributesArray[6], attributesArray[7], attributesArray[8]);
	}

	public BUSTEFieldsInputBusStop(String arrivalTime, String departureTime, String stopId, String stopSequence,
			String lat, String lng, String routeId, String shapeId, String closestShapePoint) throws IOException {
		if (arrivalTime == null || departureTime == null || stopId == null || stopSequence == null || lat == null
				|| lng == null || routeId == null || shapeId == null || closestShapePoint == null) {
			throw new IOException("The Bus Stop input file should have " + NUMBER_ATTRIBUTES
					+ " attributes not null. The fields should be separated by comma.");
		}

		initializeAttributes(arrivalTime, departureTime, stopId, stopSequence, lat, lng, routeId, shapeId,
				closestShapePoint);
	}

	private void initializeAttributes(String arrivalTime, String departureTime, String stopId, String stopSequence,
			String lat, String lng, String routeId, String shapeId, String closestShapePoint) {
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.stopId = stopId;
		this.stopSequence = stopSequence;
		this.lat = lat;
		this.lng = lng;
		this.routeId = routeId;
		this.shapeId = shapeId;
		this.closestShapePoint = closestShapePoint;
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public String getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}

	public String getStopId() {
		return stopId;
	}

	public void setStopId(String stopId) {
		this.stopId = stopId;
	}

	public String getStopSequence() {
		return stopSequence;
	}

	public void setStopSequence(String stopSequence) {
		this.stopSequence = stopSequence;
	}

	public String getLatStop() {
		return lat;
	}

	public void setLatStop(String latStop) {
		this.lat = latStop;
	}

	public String getLngStop() {
		return lng;
	}

	public void setLngStop(String lngStop) {
		this.lng = lngStop;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getShapeId() {
		return shapeId;
	}

	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}

	public String getClosestShapePoint() {
		return closestShapePoint;
	}

	public void setClosestShapePoint(String closestShapePoint) {
		this.closestShapePoint = closestShapePoint;
	}
}