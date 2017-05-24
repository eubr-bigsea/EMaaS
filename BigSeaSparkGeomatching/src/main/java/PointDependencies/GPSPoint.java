package PointDependencies;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import genericEntity.util.data.GenericObject;

public class GPSPoint  extends GeoPoint{
	
	private static final long serialVersionUID = 1L;
	//"bus.code","latitude","longitude","timestamp","line.code"
	//"AL300",-25.439896,-49.222006,2015-10-19 06:13:04,"022"
	private String gpsId;
	private String busCode;
	private String timeStamp;
	private String lineCode;
	private ShapePoint closestPoint;
	private Integer numberTrip;
	private float distanceClosestShapePoint;
	private int thresholdShape;

	public GPSPoint(String busCode, String latitude, String longitude, String timeStamp, String lineCode) {
		super(latitude, longitude);
		this.busCode = busCode;
		this.timeStamp = timeStamp;
		this.lineCode = lineCode;
	}
	
	public GPSPoint(String busCode, String latitude, String longitude, String timeStamp, String lineCode, 
			String gpsId) {
        this(busCode, latitude, longitude, timeStamp, lineCode);
        this.gpsId = gpsId;
	}
	
	public String getGpsId() {
	    return gpsId;
	}
	
	public void setGpsId(String gpsId) {
	    this.gpsId = gpsId;
	}
	
	public Integer getNumberTrip() {
		return numberTrip;
	}

	public void setNumberTrip(Integer numberTrip) {
		this.numberTrip = numberTrip;
	}

	public ShapePoint getClosestPoint() {
		return closestPoint;
	}
	
	public String getBusCode() {
		return busCode;
	}

	public void setBusCode(String busCode) {
		this.busCode = busCode;
	}
	
	public long getTime() throws ParseException {
		SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");		
		return parser.parse(this.timeStamp).getTime();
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

	public String getBlockingKeyFromTime() {
		return timeStamp.trim().substring(0, 4).replace(":", "");
	}
	
	public void setDistanceClosestShapePoint(float distance) {
		this.distanceClosestShapePoint = distance;		
	}
	
	public float getDistanceClosestShapePoint() {
		return this.distanceClosestShapePoint;
	}

	public int getThresholdShape() {		
		return this.thresholdShape;
	}
	
	public void setThresholdShape(int threshold){
		this.thresholdShape = threshold;
	}

	public static GPSPoint createGPSPoint(String line) {
		StringTokenizer st = new StringTokenizer(line, ",");
		return new GPSPoint(st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""));
	}	
	
	public static GPSPoint createGPSPointWithId(String line) {
		StringTokenizer st = new StringTokenizer(line, ",");
		return new GPSPoint(st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""),
				st.nextToken().replace("\"", ""));
	}
	
	public static GPSPoint createGPSPoint(GenericObject line) {
		return new GPSPoint(line.getData().get("bus.code").toString(),
				line.getData().get("latitude").toString(),
				line.getData().get("longitude").toString(),
				line.getData().get("timestamp").toString(),
				line.getData().get("line.code").toString(),
				line.getData().get("gps.id").toString());
				
	}
	
	public void setClosestPoint(GeoPoint closestPoint) throws Exception {
		if (!(closestPoint instanceof ShapePoint)) {
			throw new Exception("Closest point must be a type of ShapePoint.");
		}
		ShapePoint shapePoint =  (ShapePoint) closestPoint;
		this.closestPoint = shapePoint;
	}
	
		
//	@Override
//	public String toString() {
//		return "GPSPoint [gpsId=" + gpsId + ", busCode=" + busCode + ", timeStamp=" + timeStamp +"]";
//	}
	
	@Override
	public String toString() {
		return getLongitude() + "," + getLatitude() + ", timeStamp=" + timeStamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((busCode == null) ? 0 : busCode.hashCode());
		result = prime * result + ((gpsId == null) ? 0 : gpsId.hashCode());
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
		if (gpsId == null) {
			if (other.gpsId != null)
				return false;
		} else if (!gpsId.equals(other.gpsId))
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