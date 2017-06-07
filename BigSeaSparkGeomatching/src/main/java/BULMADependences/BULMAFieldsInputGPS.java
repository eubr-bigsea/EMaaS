package BULMADependences;

import java.io.IOException;
import java.io.Serializable;

public class BULMAFieldsInputGPS implements Serializable {

	private static final long serialVersionUID = 1L;
	private String busCode;
	private String latitude;
	private String longitude;
	private String timestamp;
	private String lineCode;
	private String gpsId;
	private static final int NUMBER_ATTRIBUTES = 6;

	public BULMAFieldsInputGPS(String params) throws IOException {
		String[] attributesArray = params.split(",");

		if (attributesArray.length != NUMBER_ATTRIBUTES) {
			throw new IOException(
					"The GPS input file should have 6 attributes not null. The fields should be separated by comma.");
		}

		initializeAttributes(attributesArray[0], attributesArray[1], attributesArray[2], attributesArray[3],
				attributesArray[4], attributesArray[5]);
	}

	public BULMAFieldsInputGPS(String busCode, String latitude, String longitude, String timestamp, String lineCode,
			String gpsId) throws IOException {
		if (busCode == null || latitude == null || longitude == null || timestamp == null || lineCode == null
				|| gpsId == null) {
			throw new IOException("The GPS input file should have 6 attributes not null.");
		}

		initializeAttributes(busCode, latitude, longitude, timestamp, lineCode, gpsId);
	}

	private void initializeAttributes(String busCode, String latitude, String longitude, String timestamp,
			String lineCode, String gpsId) {
		this.busCode = busCode;
		this.latitude = latitude;
		this.longitude = longitude;
		this.timestamp = timestamp;
		this.lineCode = lineCode;
		this.gpsId = gpsId;
	}

	public String getBusCode() {
		return busCode;
	}

	public void setBusCode(String busCode) {
		this.busCode = busCode;
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

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getLineCode() {
		return lineCode;
	}

	public void setLineCode(String lineCode) {
		this.lineCode = lineCode;
	}

	public String getGpsId() {
		return gpsId;
	}

	public void setGpsId(String gpsId) {
		this.gpsId = gpsId;
	}
}