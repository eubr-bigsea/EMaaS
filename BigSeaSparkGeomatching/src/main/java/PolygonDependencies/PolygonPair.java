package PolygonDependencies;

import java.io.Serializable;
import java.text.DecimalFormat;

public class PolygonPair implements Serializable{
	private GeoPolygon source;
	private GeoPolygon target;
	private double linguisticSimilarity;
	private double polygonSimilarity;
	private PolygonClassification polygonClassification;
	private DecimalFormat format = new DecimalFormat("0.00"); 
	
		
	public PolygonPair(GeoPolygon source, GeoPolygon target, double linguisticSimilarity, double polygonSimilarity,
			PolygonClassification polygonClassification) {
		super();
		this.source = source;
		this.target = target;
		this.linguisticSimilarity = linguisticSimilarity;
		this.polygonSimilarity = polygonSimilarity;
		this.polygonClassification = polygonClassification;
	}
	
	public GeoPolygon getSource() {
		return source;
	}
	public void setSource(GeoPolygon source) {
		this.source = source;
	}
	public GeoPolygon getTarget() {
		return target;
	}
	public void setTarget(GeoPolygon target) {
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
	public double getPolygonSimilarity() {
		return polygonSimilarity;
	}
	public void setPolygonSimilarity(double polygonSimilarity) {
		this.polygonSimilarity = polygonSimilarity;
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
				"Linguistic Similarity: " + format.format(linguisticSimilarity) + "\n" +
				"Polygon Similarity: " + format.format(polygonSimilarity) + "\n" +
				"Distance: " + format.format(getDistanceBetweenPolygons()) + "\n" +
				"Difference of Areas: " + format.format(getDifferenceOfArea()) + "\n" +
				"Classification: " + polygonClassification.toString();
		return output;
	}

	public String toStringCSV() {
		String output = source.getGeoName() + "(" + source.getIdInDataset() + ")" + ";" +
				target.getGeoName() + "(" + target.getIdInDataset() + ")" + ";" +
				format.format(linguisticSimilarity) + ";" +
				format.format(polygonSimilarity) + ";" +
				format.format(getDistanceBetweenPolygons()) + ";" +
				format.format(getDifferenceOfArea()) + ";" +
				polygonClassification.toString();
		return output;
	}

}
