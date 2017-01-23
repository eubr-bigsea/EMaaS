package PointDependencies;

import java.util.StringTokenizer;

public class GPSPoint extends GeoPoint{
	
	//"bus.code","latitude","longitude","timestamp","line.code"
	//"AL300",-25.439896,-49.222006,2015-10-19 06:13:04,"022"
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String busCode;
	private String timeStamp;
	private String lineCode;

	public GPSPoint(String busCode, String latitude, String longitude, String timeStamp, String lineCode) {
		super(latitude, longitude);
		this.busCode = busCode;
		this.timeStamp = timeStamp;
		this.lineCode = lineCode;
	}
	
	public static GPSPoint createGPSPoint(String line) {
		StringTokenizer st = new StringTokenizer(line, ",");
		return new GPSPoint(st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""));
	}

	public String getBusCode() {
		return busCode;
	}

	public void setBusCode(String busCode) {
		this.busCode = busCode;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getLineCode() {
		return lineCode;
	}

	public void setLineCode(String lineCode) {
		this.lineCode = lineCode;
	}

	@Override
	public String toString() {
		return "GPSPoint [busCode=" + busCode + ", timeStamp=" + timeStamp + ", lineCode=" + lineCode + super.toString() +"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((busCode == null) ? 0 : busCode.hashCode());
		result = prime * result + ((lineCode == null) ? 0 : lineCode.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GPSPoint other = (GPSPoint) obj;
		if (busCode == null) {
			if (other.busCode != null)
				return false;
		} else if (!busCode.equals(other.busCode))
			return false;
		if (lineCode == null) {
			if (other.lineCode != null)
				return false;
		} else if (!lineCode.equals(other.lineCode))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}
		
}
