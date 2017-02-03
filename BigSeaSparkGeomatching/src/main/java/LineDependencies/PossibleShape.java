package LineDependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import PointDependencies.GPSPoint;
import PointDependencies.GeoPoint;
import scala.Tuple2;

public class PossibleShape implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private ShapeLine shapeLine;
	private List<Tuple2<String, Integer>> possibleFirtGPSPoints;
	private List<Tuple2<String, Integer>> possibleLastGPSPoints;
	private List<Integer> listIndexFirstAndLastGPSPoints;
	private List<GeoPoint> listGPSPoints;
	
	public PossibleShape(List<GeoPoint> listGPSPoints, ShapeLine shapeLine) {
		this.listGPSPoints = listGPSPoints;
		this.shapeLine = shapeLine;
		this.possibleFirtGPSPoints = new ArrayList<>();
		this.possibleLastGPSPoints = new ArrayList<>();
		this.listIndexFirstAndLastGPSPoints = new ArrayList<>();
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

	public List<Tuple2<String, Integer>> getPossibleFirtsGPSPoints() {
		List<Tuple2<String, Integer>> outputList = new ArrayList<>();
		if (this.possibleFirtGPSPoints.isEmpty()) {
			return outputList;
		}
		
		Tuple2<String, Integer> tupleKeyPosition = this.possibleFirtGPSPoints.get(0);
		String lastKey = tupleKeyPosition._1;
		List<Integer> listPointsOfTheGroup = new ArrayList<>();
		listPointsOfTheGroup.add(tupleKeyPosition._2);
		for (int gpsPointIndex = 1; gpsPointIndex < this.possibleFirtGPSPoints.size(); gpsPointIndex++) {
			tupleKeyPosition = this.possibleFirtGPSPoints.get(gpsPointIndex);
			String currentKey = tupleKeyPosition._1;
			if (currentKey.equals(lastKey)) {
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			} else {
				Integer indexClosestPoint = getFirstGPSPoint(listPointsOfTheGroup);
				outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));
				lastKey = currentKey;
				listPointsOfTheGroup = new ArrayList<>();
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			}
		}
		Integer indexClosestPoint = getFirstGPSPoint(listPointsOfTheGroup);
		outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));
		return outputList;
	}

	public List<Tuple2<String, Integer>> getPossibleLastGPSPoints() {
		List<Tuple2<String, Integer>> outputList = new ArrayList<>();
		if (this.possibleLastGPSPoints.isEmpty()) {
			return outputList;
		}
		
		Tuple2<String, Integer> tupleKeyPosition = this.possibleLastGPSPoints.get(0);
		String lastKey = tupleKeyPosition._1;
		List<Integer> listPointsOfTheGroup = new ArrayList<>();
		listPointsOfTheGroup.add(tupleKeyPosition._2);
		for (int gpsPointIndex = 1; gpsPointIndex < this.possibleLastGPSPoints.size(); gpsPointIndex++) {
			tupleKeyPosition = this.possibleLastGPSPoints.get(gpsPointIndex);
			String currentKey = tupleKeyPosition._1;
			if (currentKey.equals(lastKey)) {
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			} else {
				Integer indexClosestPoint = getLastGPSPoint(listPointsOfTheGroup);
				outputList.add(new Tuple2<String, Integer>(lastKey, indexClosestPoint));
				lastKey = currentKey;
				listPointsOfTheGroup = new ArrayList<>();
				listPointsOfTheGroup.add(tupleKeyPosition._2);
			}			
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
		List<Coordinate> coordinates = new ArrayList<>();

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
		return getShapeLine().getLine().getLength() * getNumTrips();
	}

	@Override
	public String toString() {
		return "PossibleShape [" + getListIndexFirstAndLastGPSPoints() + "]";
	}	
}
