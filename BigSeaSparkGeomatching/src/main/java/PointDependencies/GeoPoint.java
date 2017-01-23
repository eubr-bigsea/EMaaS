package PointDependencies;

import java.util.ArrayList;
import java.util.List;

import scala.Serializable;

public abstract class GeoPoint implements Serializable{

	//shape: 3217,-25.4757686477818,-49.2923877163312,3281146,24.441
	//stopLine: 3167295,"06:11:12","06:11:12",25681,2,1,"Terminal Campina do Siqueira",0,301,"022","INTER 2 (HORÁRIO)",3,"FF0000","Estação Tubo Santa Quitéria",-25.459129997717,-49.302406792031,3217
	//gpsLine: "AL300",-25.440416,-49.220878,2015-10-19 06:13:33,"022"
	
	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	protected String latitude;
	protected String longitude;
	protected List<ClosestPoints> acumulator;
	
	
	public GeoPoint(String latitude, String longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.acumulator = new ArrayList<ClosestPoints>();
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
	

	public List<ClosestPoints> getAcumulator() {
		return acumulator;
	}

	public void setAcumulator(List<ClosestPoints> acumulator) {
		this.acumulator = acumulator;
	}
	public void addGeoPoint(GeoPoint point) {
		this.acumulator.add(new ClosestPoints(point, new ShapePoint(), -1.0));
	}

	@Override
	public String toString() {
		return "GeoPoint [latitude=" + latitude + ", longitude=" + longitude + "]";
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
	
	public String getBlockingKey(int range){
		String latitudePart = this.getLatitude();
		if (this.getLatitude().startsWith("-")) latitudePart = latitudePart.substring(0, range);
		else latitudePart = latitudePart.substring(0, range-1);
		String longitudePart = this.getLongitude();
		if (this.getLongitude().startsWith("-")) longitudePart = longitudePart.substring(0, range);
		else longitudePart = longitudePart.substring(0, range-1);
		return latitudePart + longitudePart;
	}
	
}
