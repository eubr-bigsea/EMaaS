package LineDependencies;

import java.util.ArrayList;
import java.util.Collections;
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

	public GPSLine(String id, LineString line, String blockingKey, List<GeoPoint> listGeoPoints,
			float greaterDistancePoints) {
		super(id, line, blockingKey, listGeoPoints, greaterDistancePoints);
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
		if (possibleShapeLine != null) {
			this.listPossibleShapeLines.add(possibleShapeLine);
		}
	}

	public List<Trip> getTrip(Integer index) throws Exception {
		if (!getMapTrips().containsKey(index)) {
			throw new Exception("The trip index doesn't exists!");
		}
		return getMapTrips().get(index);
	}

	public Map<Integer, List<Trip>> getMapTrips() {
		return this.mapTrips;
	}

	public void setUpTrips() {

		Collections.sort(getListPossibleShapeLines());

		for (PossibleShape possibleShape : getListPossibleShapeLines()) {
			if (possibleShape != null) {
				int numberTrip = 1;

				for (int i = 0; i < possibleShape.getListIndexFirstAndLastGPSPoints().size() - 1; i += 2) {
					boolean isTripProblem = false;
					int firstIndex = possibleShape.getListIndexFirstAndLastGPSPoints().get(i);
					int lastIndex = possibleShape.getListIndexFirstAndLastGPSPoints().get(i + 1);

					if (lastIndex < 0) {
						isTripProblem = true;
						firstIndex *= -1;
						lastIndex *= -1;
					}

					if (isTripProblem) {
						addTrip(numberTrip++, firstIndex, lastIndex, possibleShape.getShapeLine(),
								Problem.TRIP_PROBLEM);
					} else {
						addTrip(numberTrip++, firstIndex, lastIndex, possibleShape.getShapeLine(), Problem.NO_PROBLEM);
					}

				}
			}
		}

		setUpOutliers();
	}

	private void setUpOutliers() {

		for (int i = 1; i <= getMapTrips().keySet().size(); i++) {

			List<Trip> currentlistTrip = getMapTrips().get(i);

			if (!currentlistTrip.isEmpty()) {

				ArrayList<GeoPoint> pointsTripGPS;
				int currentLastIndex = currentlistTrip.get(currentlistTrip.size() - 1).getLastIndex();

				if (i == 1 && !currentlistTrip.isEmpty()) {
					int currentFirstIndex = currentlistTrip.get(0).getFirstIndex();
					if (currentFirstIndex > 0) {
						pointsTripGPS = new ArrayList<>();
						pointsTripGPS.addAll(this.getListGeoPoints().subList(0, currentFirstIndex));
						try {
							currentlistTrip.add(0, new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				if (i > 1 && i == getMapTrips().keySet().size()) {
					if (this.getListGeoPoints().size() - 1 > currentLastIndex) {

						pointsTripGPS = new ArrayList<>();
						pointsTripGPS.addAll(
								this.getListGeoPoints().subList(currentLastIndex + 1, this.getListGeoPoints().size()));
						try {
							currentlistTrip.add(new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				} else if (i > 1) {
					List<Trip> nextListTrip = getMapTrips().get(i + 1);
					if (!nextListTrip.isEmpty()) {
						int nextFirstIndex = nextListTrip.get(0).getFirstIndex();
						if (nextFirstIndex > currentLastIndex + 1) {

							pointsTripGPS = new ArrayList<>();
							pointsTripGPS.addAll(this.getListGeoPoints().subList(currentLastIndex + 1, nextFirstIndex));

							try {
								currentlistTrip.add(new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}

	}

	private void addTrip(Integer numberTrip, Integer firstIndex, Integer lastIndex, ShapeLine shapeLine,
			Problem problem) {
		
		if (numberTrip > 1) {		
			List<Trip> previousTrip = getMapTrips().get(numberTrip - 1);
			if (!previousTrip.isEmpty()) {
				int lastIndexPreviousTrip = previousTrip.get(previousTrip.size() - 1).getLastIndex() + 1;
				if (firstIndex < lastIndexPreviousTrip) {
					firstIndex = lastIndexPreviousTrip;
				}
			}
		}
		
		if (numberTrip < getMapTrips().keySet().size()) {
			List<Trip> nextTrip = getMapTrips().get(numberTrip + 1);
			if (!nextTrip.isEmpty()) {
				int firstIndexNextTrip = nextTrip.get(0).getFirstIndex();
				if (lastIndex >= firstIndexNextTrip) {
					lastIndex = firstIndexNextTrip;
				}
			}
		}

		if (!getMapTrips().containsKey(numberTrip)) {
			getMapTrips().put(numberTrip, new ArrayList<Trip>());
		}

		List<Trip> listTrips = getMapTrips().get(numberTrip);
		ArrayList<GeoPoint> pointsTripGPS;
		Trip newTrip = null;

		if (!listTrips.isEmpty()) {
			int indexPreviousLastPoint = listTrips.get(listTrips.size() - 1).getLastIndex() + 1;
			if (firstIndex > indexPreviousLastPoint) {

				pointsTripGPS = new ArrayList<>();
				pointsTripGPS.addAll(this.getListGeoPoints().subList(indexPreviousLastPoint, firstIndex));

				try {
					newTrip = new Trip(null, pointsTripGPS, Problem.OUTLIER_POINT);
					newTrip.setFirstIndex(indexPreviousLastPoint);
					newTrip.setLastIndex(firstIndex);
					listTrips.add(newTrip);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (firstIndex < indexPreviousLastPoint) {
				firstIndex = indexPreviousLastPoint;
			}
		}

		if (firstIndex < lastIndex) {
			pointsTripGPS = new ArrayList<>();
			pointsTripGPS.addAll(this.getListGeoPoints().subList(firstIndex, lastIndex + 1));

			try {
				newTrip = new Trip(shapeLine, pointsTripGPS, problem);
				newTrip.setFirstIndex(firstIndex);
				newTrip.setLastIndex(lastIndex);
				listTrips.add(newTrip);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		return "GPS[ BlockingKey: " + getId() + getListPossibleShapeLines() + "]";
	}
}
