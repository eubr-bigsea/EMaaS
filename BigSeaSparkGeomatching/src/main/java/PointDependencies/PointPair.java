package PointDependencies;

import java.io.Serializable;
import java.text.DecimalFormat;

import PolygonDependencies.PolygonClassification;

public class PointPair implements Serializable{
	private GeoPoint2 source;
	private GeoPoint2 target;
	private double linguisticSimilarity;
	private double distance;
	private PolygonClassification polygonClassification;
	private DecimalFormat format = new DecimalFormat("0.0000000"); 
	
		
	public PointPair(GeoPoint2 source, GeoPoint2 target, double linguisticSimilarity, double distance,
			PolygonClassification polygonClassification) {
		super();
		this.source = source;
		this.target = target;
		this.linguisticSimilarity = linguisticSimilarity;
		this.distance = distance;
		this.polygonClassification = polygonClassification;
	}
	
	public GeoPoint2 getSource() {
		return source;
	}
	public void setSource(GeoPoint2 source) {
		this.source = source;
	}
	public GeoPoint2 getTarget() {
		return target;
	}
	public void setTarget(GeoPoint2 target) {
		this.target = target;
	}
	public PolygonClassification getPolygonClassification() {
		return polygonClassification;
	}
	public void setPolygonClassification(PolygonClassification polygonClassification) {
		this.polygonClassification = polygonClassification;
	}
	public double getLinguisticSimilarity() {
		return linguisticSimilarity;
	}
	public void setLinguisticSimilarity(double linguisticSimilarity) {
		this.linguisticSimilarity = linguisticSimilarity;
	}
	public double getPointSimilarity() {
		return distance;
	}
	public void setPointSimilarity(double distance) {
		this.distance = distance;
	}
	
	public double getAreaTarget() {
		return target.getArea();
	}
	public double getAreaSource() {
		return source.getArea();
	}
	public double getDifferenceOfArea() {
		return Math.abs(getAreaTarget() - getAreaSource());
	}
	/**
	 * Calculate the distance between both polygons.
	 * @return distance (in meters).
	 */
	public double getDistanceBetweenPolygons() {
		return source.getCentroid().distance(target.getCentroid());
	}
	
	@Override
	public String toString() {
		String output = source.getGeoName() + "(" + source.getIdInDataset() + ")" + " - " + target.getGeoName() + "(" + target.getIdInDataset() + ")" + "\n" +
				"Distance: " + format.format(getPointSimilarity()) + "\n" +
				"Classification: " + polygonClassification.toString();
		return output;
	}

	public String toStringCSV() {
		String output = source.getGeoName() + "(" + source.getIdInDataset() + ")" + ";" +
				target.getGeoName() + "(" + target.getIdInDataset() + ")" + ";" +
				format.format(getPointSimilarity()) + ";" +
				polygonClassification.toString();
		return output;
	}

}
