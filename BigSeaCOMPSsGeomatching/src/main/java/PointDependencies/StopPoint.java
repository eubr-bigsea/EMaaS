package PointDependencies;

import java.util.StringTokenizer;

public class StopPoint extends GeoPoint{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//"trip_id","arrival_time","departure_time","stop_id","stop_sequence","service_id","trip_headsign","direction_id","route_id","route_short_name","route_long_name","route_type","route_color","stop_name","stop_lat","stop_lon","shape_id"
	//3167295,"06:04:00","06:04:00",26240,1,1,"Terminal Campina do Siqueira",0,301,"022","INTER 2 (HORÁRIO)",3,"FF0000","Terminal Portão - 022 - Inter 2 (Horário)",-25.475997037397,-49.292383487269,3217
	private String tripId;
	private String arrivalTime;
	private String departureTime;
	private String stopSequence;
	private String serviceId;
	private String tripHeadsign;
	private String directionId;
	private String routeId;
	private String routeShortName;
	private String routeLongName;
	private String routeType;
	private String routeColor;
	private String stopName;
	private String shapeId;
	
	public StopPoint(String tripId, String arrivalTime, String departureTime, String stopSequence, 
			String serviceId, String tripHeadsign, String directionId, String routeId,
			String routeShortName, String routeLongName, String routeType, String routeColor, 
			String stopName, String latitude, String longitude, String shapeId) {
		
		super(latitude, longitude);
		this.tripId = tripId;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.stopSequence = stopSequence;
		this.serviceId = serviceId;
		this.tripHeadsign = tripHeadsign;
		this.directionId = directionId;
		this.routeId = routeId;
		this.routeShortName = routeShortName;
		this.routeLongName = routeLongName;
		this.routeType = routeType;
		this.routeColor = routeColor;
		this.stopName = stopName;
		this.shapeId = shapeId;
	}
	
	public static StopPoint createStopPoint(String line) {
		StringTokenizer st = new StringTokenizer(line, ",");
		return new StopPoint(st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""), 
				st.nextToken().replace("\"", ""));
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
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

	public String getStopSequence() {
		return stopSequence;
	}

	public void setStopSequence(String stopSequence) {
		this.stopSequence = stopSequence;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getTripHeadsign() {
		return tripHeadsign;
	}

	public void setTripHeadsign(String tripHeadsign) {
		this.tripHeadsign = tripHeadsign;
	}

	public String getDirectionId() {
		return directionId;
	}

	public void setDirectionId(String directionId) {
		this.directionId = directionId;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getRouteShortName() {
		return routeShortName;
	}

	public void setRouteShortName(String routeShortName) {
		this.routeShortName = routeShortName;
	}

	public String getRouteLongName() {
		return routeLongName;
	}

	public void setRouteLongName(String routeLongName) {
		this.routeLongName = routeLongName;
	}

	public String getRouteType() {
		return routeType;
	}

	public void setRouteType(String routeType) {
		this.routeType = routeType;
	}

	public String getRouteColor() {
		return routeColor;
	}

	public void setRouteColor(String routeColor) {
		this.routeColor = routeColor;
	}

	public String getStopName() {
		return stopName;
	}

	public void setStopName(String stopName) {
		this.stopName = stopName;
	}

	public String getShapeId() {
		return shapeId;
	}

	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}

	@Override
	public String toString() {
		return "StopPoint [tripId=" + tripId + ", arrivalTime=" + arrivalTime + ", departureTime=" + departureTime
				+ ", stopSequence=" + stopSequence + ", serviceId=" + serviceId + ", tripHeadsign=" + tripHeadsign
				+ ", directionId=" + directionId + ", routeId=" + routeId + ", routeShortName=" + routeShortName
				+ ", routeLongName=" + routeLongName + ", routeType=" + routeType + ", routeColor=" + routeColor
				+ ", stopName=" + stopName + ", shapeId=" + shapeId + super.toString() +"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((arrivalTime == null) ? 0 : arrivalTime.hashCode());
		result = prime * result + ((departureTime == null) ? 0 : departureTime.hashCode());
		result = prime * result + ((directionId == null) ? 0 : directionId.hashCode());
		result = prime * result + ((routeColor == null) ? 0 : routeColor.hashCode());
		result = prime * result + ((routeId == null) ? 0 : routeId.hashCode());
		result = prime * result + ((routeLongName == null) ? 0 : routeLongName.hashCode());
		result = prime * result + ((routeShortName == null) ? 0 : routeShortName.hashCode());
		result = prime * result + ((routeType == null) ? 0 : routeType.hashCode());
		result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
		result = prime * result + ((shapeId == null) ? 0 : shapeId.hashCode());
		result = prime * result + ((stopName == null) ? 0 : stopName.hashCode());
		result = prime * result + ((stopSequence == null) ? 0 : stopSequence.hashCode());
		result = prime * result + ((tripHeadsign == null) ? 0 : tripHeadsign.hashCode());
		result = prime * result + ((tripId == null) ? 0 : tripId.hashCode());
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
		StopPoint other = (StopPoint) obj;
		if (arrivalTime == null) {
			if (other.arrivalTime != null)
				return false;
		} else if (!arrivalTime.equals(other.arrivalTime))
			return false;
		if (departureTime == null) {
			if (other.departureTime != null)
				return false;
		} else if (!departureTime.equals(other.departureTime))
			return false;
		if (directionId == null) {
			if (other.directionId != null)
				return false;
		} else if (!directionId.equals(other.directionId))
			return false;
		if (routeColor == null) {
			if (other.routeColor != null)
				return false;
		} else if (!routeColor.equals(other.routeColor))
			return false;
		if (routeId == null) {
			if (other.routeId != null)
				return false;
		} else if (!routeId.equals(other.routeId))
			return false;
		if (routeLongName == null) {
			if (other.routeLongName != null)
				return false;
		} else if (!routeLongName.equals(other.routeLongName))
			return false;
		if (routeShortName == null) {
			if (other.routeShortName != null)
				return false;
		} else if (!routeShortName.equals(other.routeShortName))
			return false;
		if (routeType == null) {
			if (other.routeType != null)
				return false;
		} else if (!routeType.equals(other.routeType))
			return false;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equals(other.serviceId))
			return false;
		if (shapeId == null) {
			if (other.shapeId != null)
				return false;
		} else if (!shapeId.equals(other.shapeId))
			return false;
		if (stopName == null) {
			if (other.stopName != null)
				return false;
		} else if (!stopName.equals(other.stopName))
			return false;
		if (stopSequence == null) {
			if (other.stopSequence != null)
				return false;
		} else if (!stopSequence.equals(other.stopSequence))
			return false;
		if (tripHeadsign == null) {
			if (other.tripHeadsign != null)
				return false;
		} else if (!tripHeadsign.equals(other.tripHeadsign))
			return false;
		if (tripId == null) {
			if (other.tripId != null)
				return false;
		} else if (!tripId.equals(other.tripId))
			return false;
		return true;
	}
	
}
