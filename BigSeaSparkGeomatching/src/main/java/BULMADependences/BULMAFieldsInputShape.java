package BULMADependences;

import java.io.IOException;
import java.io.Serializable;

public class BULMAFieldsInputShape implements Serializable {

	private static final long serialVersionUID = 1L;
	private String route;
	private String shapeId;
	private String latitude;
	private String longitude;
	private String sequence;
	private String distanceTraveled;
	private static final int NUMBER_ATTRIBUTES = 6;

	public BULMAFieldsInputShape(String params) throws IOException {
		String[] attributesArray = params.split(",");

		if (attributesArray.length != NUMBER_ATTRIBUTES) {
			throw new IOException(
					"The Shape input file should have 6 attributes not null. The fields should be separated by comma.");
		}

		initializeAttributes(attributesArray[0], attributesArray[1], attributesArray[2], attributesArray[3],
				attributesArray[4], attributesArray[5]);
	}

	public BULMAFieldsInputShape(String route, String shapeId, String latitude, String longitude, String sequence,
			String distanceTraveled) throws IOException {

		if (route == null || shapeId == null || latitude == null || longitude == null || sequence == null
				|| distanceTraveled == null) {
			throw new IOException("The Shape input file should have 6 attributes not null.");
		}

		initializeAttributes(route, shapeId, latitude, longitude, sequence, distanceTraveled);
	}

	private void initializeAttributes(String route, String shapeId, String latitude, String longitude, String sequence,
			String distanceTraveled) {
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