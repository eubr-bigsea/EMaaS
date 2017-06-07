package BULMADependences;

public class BULMAInputGPS {
	
	String busCode;
	String latitude;
	String longitude;
	String timestamp;
	String lineCode;
	String gpsId;

	public BULMAInputGPS(String params) {
		// TODO split params to set attributes 
	}

	public BULMAInputGPS(String busCode, String latitude, String longitude, String timestamp, String lineCode, String gpsId) {
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
