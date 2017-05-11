package PolygonMatching20;

import java.io.Serializable;

import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class GeoPolygonDF implements Serializable, Cloneable{
	private Geometry geometry;
	private Integer idGeometry;
	private String geoName;
	private String type;
	private Integer idInDataset;
	private boolean isDuplicated;//Used in single matching

	//Used in polygons matching
	public GeoPolygonDF(String geometryGIS, String geoName, String type, Integer idGeometry, Integer idInDataset) throws ParseException {
		super();
		WKTReader wktReader = new WKTReader(JtsSpatialContext.GEO.getGeometryFactory());
		Geometry geometry = wktReader.read(geometryGIS);
		this.geometry = geometry;
		this.geoName = geoName;
		this.type = type;
		this.idGeometry = idGeometry;
		this.idInDataset = idInDataset;
	}

	/**
	 * Calculate the distance between two Geometries in meters.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return the distance.
	 */
	public double getDistance(GeoPolygonDF g2) {
		return geometry.distance(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry contains the specified geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean contains(GeoPolygonDF g2) {
		return geometry.contains(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry is covered by the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean coveredBy(GeoPolygonDF g2) {
		return geometry.coveredBy(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry covers the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean covers(GeoPolygonDF g2) {
		return geometry.covers(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry crosses the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean crosses(GeoPolygonDF g2) {
		return geometry.crosses(g2.getGeometry());
	}

	/**
	 * Returns true if this geometry intersects the other geometry.
	 * 
	 * @param the
	 *            other geometry to be compared.
	 * @return boolean
	 */
	public boolean intersects(GeoPolygonDF g2) {
		return geometry.intersects(g2.getGeometry());
	}
	
	public IntersectionMatrix relate(GeoPolygonDF g2) {
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
	public boolean equalsExact(GeoPolygonDF g2, double tolerance) {
		return geometry.equalsExact(g2.getGeometry(), tolerance);
	}

	public Geometry getGeometry() {
		return geometry;
	}

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

	public String getGeoName() {
		return geoName;
	}

	public void setGeoName(String geoName) {
		this.geoName = geoName;
	}

	public Integer getIdInDataset() {
		return idInDataset;
	}

	public void setIdInDataset(Integer idInDataset) {
		this.idInDataset = idInDataset;
	}

	public Integer getIdGeometry() {
		return idGeometry;
	}

	public void setIdGeometry(Integer idGeometry) {
		this.idGeometry = idGeometry;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
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
	
	public GeoPolygonDF getGeoPolygon() throws CloneNotSupportedException {
	    return  (GeoPolygonDF) this.clone();
	}
	
	/**
	 * This method calculates the proportion of common area between two polygons.
	 * The common area is compared in relation the areas of each polygon.
	 * The worst case is returned since it represents better the similarity between the two polygons.
	 * @param otherGeometry
	 * @return the proportion of common area between two polygons
	 */
	public double getPolygonSimilarity(GeoPolygonDF otherGeometry){
		Geometry intersectionGeometry = geometry.intersection(otherGeometry.getGeometry());
		double proportionOfCurrentPolygon = intersectionGeometry.getArea()/geometry.getArea();
		double proportionOfOtherPolygon = intersectionGeometry.getArea()/otherGeometry.getGeometry().getArea();
		if (proportionOfCurrentPolygon < proportionOfOtherPolygon) {
			return proportionOfCurrentPolygon;
		} else {
			return proportionOfOtherPolygon;
		}
	}

	

}
