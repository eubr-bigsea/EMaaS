package PointDependencies;

import java.io.Serializable;

import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import LineDependencies.GeoObject;
import PolygonDependencies.InputTypes;

public class GeoPoint2 extends GeoObject implements Serializable, Cloneable, Comparable<GeoPoint2>{

	private boolean isDuplicated;//Used in single matching
	private double distanceToPOI;//distance to bustop (context matching)

	public GeoPoint2(String geometryGIS) throws ParseException {
		super();
		WKTReader wktReader = new WKTReader(JtsSpatialContext.GEO.getGeometryFactory());
		Geometry geometry = wktReader.read(geometryGIS);
		this.geometry = geometry;
	}

	public GeoPoint2(Double latitude, Double longitude) throws ParseException {
		super();
		Coordinate coordinate = new Coordinate(latitude, longitude);
		GeometryFactory geometryFactory = JtsSpatialContext.GEO.getGeometryFactory();
		Point point = geometryFactory.createPoint(coordinate);
		this.geometry = point;
	}
	

	public GeoPoint2(String geometryGIS, String geoName) throws ParseException {
		super();
		WKTReader wktReader = new WKTReader(JtsSpatialContext.GEO.getGeometryFactory());
		Geometry geometry = wktReader.read(geometryGIS);
		this.geometry = geometry;
		this.geoName = geoName;
	}
	
	//Used in polygons matching
	public GeoPoint2(String geometryGIS, String geoName, InputTypes type, Integer idGeometry, Integer idInDataset) throws ParseException {
		super(geometryGIS, type, idGeometry, geoName, idInDataset);
	}

	/**
	 * Calculate the distance between two Geometries in meters.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return the distance.
	 */
	public double getDistance(GeoPoint2 g2) {
		return geometry.distance(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry contains the specified geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean contains(GeoPoint2 g2) {
		return geometry.contains(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry is covered by the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean coveredBy(GeoPoint2 g2) {
		return geometry.coveredBy(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry covers the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean covers(GeoPoint2 g2) {
		return geometry.covers(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry crosses the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean crosses(GeoPoint2 g2) {
		return geometry.crosses(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry intersects the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean intersects(GeoPoint2 g2) {
		return geometry.intersects(g2.getGeometry());
	}
	
	public IntersectionMatrix relate(GeoPoint2 g2) {
		return geometry.relate(g2.getGeometry());
	}

	/**
	 * Returns true if the two geometries are exactly equal, up to a specified
	 * distance tolerance (in meters).
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @param the
	 *            tolerance value.
	 * @return boolean.
	 */
	public boolean equalsExact(GeoPoint2 g2, double tolerance) {
		return geometry.equalsExact(g2.getGeometry(), tolerance);
	}
	@Override
	public Geometry getGeometry() {
		return geometry;
	}
	@Override
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public int getSRID() {
		return geometry.getSRID();
	}

	public void setSRID(int SRID) {
		this.geometry.setSRID(SRID);
		;
	}

	@Override
	public String getGeoName() {
		return geoName;
	}

	@Override
	public void setGeoName(String geoName) {
		this.geoName = geoName;
	}

	@Override
	public Integer getIdInDataset() {
		return idInDataset;
	}

	@Override
	public void setIdInDataset(Integer idInDataset) {
		this.idInDataset = idInDataset;
	}

	@Override
	public Integer getIdGeometry() {
		return idGeometry;
	}

	@Override
	public void setIdGeometry(Integer idGeometry) {
		this.idGeometry = idGeometry;
	}
	@Override
	public InputTypes getType() {
		return type;
	}
	@Override
	public void setType(InputTypes type) {
		this.type = type;
	}
	
	public double getArea() {
		return geometry.getArea();
	}
	
	public Point getCentroid() {
		return geometry.getCentroid();
	}
	public boolean isDuplicated() {
		return isDuplicated;
	}

	public void setDuplicated(boolean isDuplicated) {
		this.isDuplicated = isDuplicated;
	}
	
	public GeoPoint2 getGeoPolygon() throws CloneNotSupportedException {
	    return  (GeoPoint2) this.clone();
	}
	
	public double getDistanceToPOI() {
		return distanceToPOI;
	}

	public void setDistanceToPOI(double distanceToPOI) {
		this.distanceToPOI = distanceToPOI;
	}

	/**
	 * This method calculates the proportion of common area between two polygons.
	 * The common area is compared in relation the areas of each polygon.
	 * The worst case is returned since it represents better the similarity between the two polygons.
	 * @param otherGeometry
	 * @return the proportion of common area between two polygons
	 */
	public double getPointDistance(GeoPoint2 otherGeometry){
		return geometry.distance(otherGeometry.getGeometry());
	}
	
	

	public float getPointDistanceInMeters(GeoPoint2 otherGeometry) {
		double lat1 = getGeometry().getCoordinate().x;
		double long1 = getGeometry().getCoordinate().y;
		double lat2 = otherGeometry.getGeometry().getCoordinate().x;
		double long2 = otherGeometry.getGeometry().getCoordinate().y;

		double earthRadius = 6371000; // meters
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(long2 - long1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		float dist = (float) (earthRadius * c);

		return dist;
	}

	@Override
	public int compareTo(GeoPoint2 other) {
		if (distanceToPOI < other.getDistanceToPOI()) {
			return -1;
		} else if (distanceToPOI > other.getDistanceToPOI()) {
			return 1;
		} else {
			return 0;
		}
	}
	

}
