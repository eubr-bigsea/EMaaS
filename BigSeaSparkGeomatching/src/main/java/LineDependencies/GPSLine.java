package LineDependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.LineString;

import PointDependencies.GeoPoint;

public class GPSLine extends GeoLine {
	
	private static final long serialVersionUID = 1L;
	
	private List<PossibleShape> listPossibleShapeLines;
	private Map<Integer, List<Trip>> mapTrips;
	
	public GPSLine(String id, LineString line, String blockingKey) {
		super(id, line, blockingKey);
		this.listPossibleShapeLines = new ArrayList<PossibleShape>();
		this.mapTrips = new HashMap<Integer, List<Trip>>();
	}
	
	public GPSLine(String id, LineString line, String blockingKey, List<GeoPoint> listGeoPoints) {
		super(id, line, blockingKey, listGeoPoints);
		this.listPossibleShapeLines = new ArrayList<PossibleShape>();
		this.mapTrips = new HashMap<Integer, List<Trip>>();
	}
		
	public List<PossibleShape> getListPossibleShapeLines() {
		return this.listPossibleShapeLines;
	}

	public void setListPossibleShapeLines(List<PossibleShape> listPossibleShapeLines) {
		this.listPossibleShapeLines = listPossibleShapeLines;
	}

	public void addPossibleShapeLine(PossibleShape possibleShapeLine) {
		if(possibleShapeLine != null) {
			this.listPossibleShapeLines.add(possibleShapeLine);
		}
	}
	
	public List<Trip> getTrip(Integer index) throws Exception{
		if(!getMapTrips().containsKey(index)) {
			throw new Exception("The trip index doesn't exists!");
		} 
		return getMapTrips().get(index); 
	}
	
	public Map<Integer, List<Trip>> getMapTrips() {
		return this.mapTrips;
	}
	
	public void setUpTrips() {		
		for (PossibleShape possibleShape : getListPossibleShapeLines()) {
			int numberTrip = 1;				
			for (int i = 0; i < possibleShape.getListIndexFirstAndLastGPSPoints().size()-1; i += 2) {
                int firstIndex = possibleShape.getListIndexFirstAndLastGPSPoints().get(i);
                int lastIndex = possibleShape.getListIndexFirstAndLastGPSPoints().get(i+1)+1;                                                
                addTrip(numberTrip++, firstIndex, lastIndex, possibleShape.getShapeLine());
	        } 
		}	
	}
	
	private void addTrip(Integer numberTrip, Integer firstIndex, Integer lastIndex, ShapeLine shapeLine) {
		if (!getMapTrips().containsKey(numberTrip)) {
			getMapTrips().put(numberTrip, new ArrayList<>());
		}		
		List<Trip> listTrips = getMapTrips().get(numberTrip);	
		ArrayList<GeoPoint> pointsTripGPS = new ArrayList<>();
		pointsTripGPS.addAll(this.getListGeoPoints().subList(firstIndex, lastIndex));
		Trip newTrip = null;
		try {
			newTrip = new Trip(shapeLine, pointsTripGPS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		listTrips.add(newTrip);		
	}
	
	@Override
	public String toString() {		
		return "GPS[ BlockingKey: " + getBlockingKey() + getListGeoPoints() +"]";
	}
}
