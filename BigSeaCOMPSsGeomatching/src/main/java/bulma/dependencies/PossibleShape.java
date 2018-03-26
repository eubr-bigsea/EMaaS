package bulma.dependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import scala.Tuple2;

public class PossibleShape implements Serializable, Comparable<PossibleShape> {

	private static final long serialVersionUID = 1L;
	
	private ShapeLine shapeLine;
	private Queue<Tuple2<String, Integer>> possibleFirtGPSPoints;
	private Queue<Tuple2<String, Integer>> possibleLastGPSPoints;
	private List<Integer> listIndexFirstAndLastGPSPoints;
	private List<GeoPoint> listGPSPoints;
	
	public PossibleShape() {
		super();
	}
	
	public PossibleShape(List<GeoPoint> listGPSPoints, ShapeLine shapeLine) {
		this.listGPSPoints = listGPSPoints;
		this.shapeLine = shapeLine;
		this.possibleFirtGPSPoints = new LinkedList<Tuple2<String, Integer>>();
		this.possibleLastGPSPoints = new LinkedList<Tuple2<String, Integer>>();
		this.listIndexFirstAndLastGPSPoints = new ArrayList<Integer>();
	}

	public void addPossibleFirstPoint(Tuple2<String, Integer> possiblePoint) {
		this.possibleFirtGPSPoints.add(possiblePoint);
	}

	public void addFirstAndLastPoint(Integer indexPoint) {
		this.listIndexFirstAndLastGPSPoints.add(indexPoint);
	}

	public ShapeLine getShapeLine() {
		return this.shapeLine;
	}

	public void setShapeLine(ShapeLine shapeLine) {
		this.shapeLine = shapeLine;
	}

	public List<GeoPoint> getListGPSPoints() {
		return this.listGPSPoints;
	}
	
	public Queue<Tuple2<String, Integer>> getListPossibleFirtsGPSPoints() {
		return this.possibleFirtGPSPoints;
	}
	
	public Queue<Tuple2<String, Integer>> getListPossibleLastGPSPoints() {
		return this.possibleLastGPSPoints;
	}
	

	public Queue<Tuple2<String, Integer>> getFirstGPSPoints() {
		
		if (this.possibleFirtGPSPoints.isEmpty()) {
			return this.possibleFirtGPSPoints;
		}
		
		Queue<Tuple2<String, Integer>> outputList = new LinkedList<Tuple2<String, Integer>>();
		Tuple2<String, Integer> tupleKeyPosition = null;
		String lastKey = null;
		List<Integer> listPointsOfTheGroup = new ArrayList<Integer>();
		
					
		while(!this.possibleFirtGPSPoints.isEmpty()) {
			tupleKeyPosition = this.possibleFirtGPSPoints.poll();
			String currentKey = tupleKeyPosition._1;
			if (lastKey == null || currentKey.equals(lastKey)) {
				listPointsOfTheGroup.add(tupleKeyPosition._2);
				
			} else{
				Integer indexClosestPoint = getFirstGPSPoint(listPointsOfTheGroup);
				outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));
				listPointsOfTheGroup = new ArrayList<Integer>();
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			}
			lastKey = currentKey;
		}
		Integer indexClosestPoint = getFirstGPSPoint(listPointsOfTheGroup);
		outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));
		return outputList;
	}

	public Queue<Tuple2<String, Integer>> getLastGPSPoints() {
			
		if (this.possibleLastGPSPoints.isEmpty()) {
			return this.possibleLastGPSPoints;
		}
		
		Queue<Tuple2<String, Integer>> outputList = new LinkedList<Tuple2<String, Integer>>();		
		Tuple2<String, Integer> tupleKeyPosition = null;
		String lastKey = null;
		List<Integer> listPointsOfTheGroup = new ArrayList<Integer>();
		
		
		while(!this.possibleLastGPSPoints.isEmpty()) {
			tupleKeyPosition = this.possibleLastGPSPoints.poll();
			String currentKey = tupleKeyPosition._1;
			if (lastKey == null || currentKey.equals(lastKey)) {
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			} else {
				Integer indexClosestPoint = getLastGPSPoint(listPointsOfTheGroup);
				outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));				
				listPointsOfTheGroup = new ArrayList<Integer>();
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			}	
			lastKey = currentKey;
		}
		Integer indexClosestPoint = getLastGPSPoint(listPointsOfTheGroup);
		outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));
		
		return outputList;
	}

	public void addPossibleLastPoint(Tuple2<String, Integer> possibleLastPoint) {
		if(possibleLastPoint != null) {
			this.possibleLastGPSPoints.add(possibleLastPoint);
		}
	}

	public List<Integer> getListIndexFirstAndLastGPSPoints() {
		return this.listIndexFirstAndLastGPSPoints;
	}
	
	public Integer getNumTrips() {
		return getListIndexFirstAndLastGPSPoints().size()/2;
	}

	public LineString createLineString(int firstIndex, int lastIndex) {
        return createLineString(getListGPSPoints().subList(firstIndex, lastIndex));
	}
	
	private LineString createLineString(List<GeoPoint> listGeoPoint) {
		@SuppressWarnings("deprecation")
		GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();
		List<Coordinate> coordinates = new ArrayList<Coordinate>();

		for (GeoPoint geoPoint : listGeoPoint) {
			coordinates.add(new Coordinate(Double.valueOf(geoPoint.getLatitude()), 
					Double.valueOf(geoPoint.getLongitude())));
		}
		Coordinate[] array = new Coordinate[coordinates.size()];
		return geometryFactory.createLineString(coordinates.toArray(array));
	}

	public float getDistanceInMetersToStartPointShape(GPSPoint gpsPoint) {
		double lat = getShapeLine().getLine().getStartPoint().getX();
		double lng = getShapeLine().getLine().getStartPoint().getY();
		return GeoPoint.getDistanceInMeters(Double.valueOf(gpsPoint.getLatitude()), 
				Double.valueOf(gpsPoint.getLongitude()), lat, lng);
	}

	public float getDistanceInMetersToEndPointShape(GPSPoint gpsPoint) {
		double lat = getShapeLine().getLine().getEndPoint().getX();
		double lng = getShapeLine().getLine().getEndPoint().getY();
		return GeoPoint.getDistanceInMeters(Double.valueOf(gpsPoint.getLatitude()), 
				Double.valueOf(gpsPoint.getLongitude()), lat, lng);
	}

	private Integer getFirstGPSPoint(List<Integer> listGroups) {
		if (listGroups.size() == 1) {
			return listGroups.get(0);
		}

		Integer closestPoint = listGroups.get(0);
		for (Integer position : listGroups) {

			float currentDistance = getDistanceInMetersToStartPointShape(
					(GPSPoint) getListGPSPoints().get(position));
			float closestDistance = getDistanceInMetersToStartPointShape(
					(GPSPoint) getListGPSPoints().get(closestPoint));
			if (currentDistance < closestDistance) {
				closestPoint = position;
			}
		}
		return closestPoint;
	}

	private Integer getLastGPSPoint(List<Integer> listGroups) {
		if (listGroups.size() == 1) {
			return listGroups.get(0);
		}

		Integer closestPoint = listGroups.get(0);
		for (Integer position : listGroups) {
			float currentDistance = getDistanceInMetersToEndPointShape(
					(GPSPoint) getListGPSPoints().get(position));
			float closestDistance = getDistanceInMetersToEndPointShape(
					(GPSPoint) getListGPSPoints().get(closestPoint));
			if (currentDistance < closestDistance) {
				closestPoint = position;
			}
		}
		return closestPoint;
	}

	public Double getLengthCoverage() {		
		return getShapeLine().getDistanceTraveled() * getNumTrips();
	}

	@Override
	public String toString() {
		return "PossibleShape (" + getFirstGPSPoints() + getLastGPSPoints()+ ")";
	}
	
	public int compareTo(PossibleShape otherShape) {
		if (this.listIndexFirstAndLastGPSPoints.size() == 0 && otherShape.getListIndexFirstAndLastGPSPoints().size() == 0) {
			return 0;
		}
		
		if (this.listIndexFirstAndLastGPSPoints.size() == 0) {
			return -1;
		}
		
		if (otherShape.getListIndexFirstAndLastGPSPoints().size() == 0){
			return 1;
		}
		
		if (Math.abs(this.listIndexFirstAndLastGPSPoints.get(0)) < Math.abs(otherShape.getListIndexFirstAndLastGPSPoints().get(0))) {
			return -1;
		}

		if (Math.abs(this.listIndexFirstAndLastGPSPoints.get(0)) > Math.abs(otherShape.getListIndexFirstAndLastGPSPoints().get(0))){
			return 1;
		}
		return 0;
	}

	public boolean isRoundShape() {
		return GeoPoint.getDistanceInMeters(getShapeLine().getFirstPoint(), getShapeLine().getLastPoint()) <= getShapeLine().getGreaterDistancePoints();
	}
}