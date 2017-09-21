package bulma.dependencies;

import java.io.Serializable;
import java.util.LinkedList;

import com.vividsolutions.jts.geom.Point;


public abstract class GeoPoint implements Serializable{

	private static final long serialVersionUID = 1L;
	private String latitude;
	private String longitude;
	private LinkedList<ClosestPoints> acumulator;
	
	public GeoPoint(String latitude, String longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.acumulator = new LinkedList<ClosestPoints>();
	}
	
	public void addFirst(){
		this.acumulator.add(new ClosestPoints(this, new ShapePoint(), -1.0));
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
	
	public LinkedList<ClosestPoints> getAcumulator() {
		return acumulator;
	}

	public void setAcumulator(LinkedList<ClosestPoints> acumulator) {
		this.acumulator = acumulator;
	}
	
	public void addGeoPoint(GeoPoint point) {
		this.acumulator.add(new ClosestPoints(point, new ShapePoint(), -1.0));
	}
	
	public String getBlockingKey(int range){
		String latitudePart = this.getLatitude();
		if(latitudePart.length() < range) {
			for (int i = 0; i < range - this.getLatitude().length(); i++) {
				latitudePart += "*";
			}
		}
		
		if (this.getLatitude().startsWith("-")) {
			latitudePart = latitudePart.substring(0, range);
		} else {
			latitudePart = latitudePart.substring(0, range-1);
		}
		
		String longitudePart = this.getLongitude();
		if(longitudePart.length() < range) {
			for (int i = 0; i < range - this.getLongitude().length(); i++) {
				longitudePart += "*";
			}
		}
		
		if (this.getLongitude().startsWith("-")) {
			longitudePart = longitudePart.substring(0, range);
		} else {
			longitudePart = longitudePart.substring(0, range-1);
		}
		
		return latitudePart + longitudePart;
	}
	
	public static float getDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
		final double earthRadius = 6371000; // meters
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		float dist = (float) (earthRadius * c);

		return dist;
	}
	
	public static float getDistanceInMeters(Point point1, Point point2) {
		return getDistanceInMeters(point1.getX(), point1.getY(), point2.getX(), point2.getY());
	}
	
	public static float getDistanceInMeters(GeoPoint point1, GeoPoint point2) {
		return getDistanceInMeters(Double.valueOf(point1.getLatitude()), Double.valueOf(point1.getLongitude()), 
				Double.valueOf(point2.getLatitude()), Double.valueOf(point2.getLongitude()));
	}
		
	@Override
	public String toString() {
		return "(" + latitude + " " + longitude + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeoPoint other = (GeoPoint) obj;
		if (latitude == null) {
			if (other.latitude != null)
				return false;
		} else if (!latitude.equals(other.latitude))
			return false;
		if (longitude == null) {
			if (other.longitude != null)
				return false;
		} else if (!longitude.equals(other.longitude))
			return false;
		return true;
	}
}