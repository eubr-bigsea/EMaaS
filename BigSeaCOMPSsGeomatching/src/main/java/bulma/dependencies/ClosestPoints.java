package bulma.dependencies;

import scala.Serializable;

public class ClosestPoints implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private GeoPoint point1;
	private GeoPoint point2;
	private double closestDistance;
	
	public ClosestPoints() {
		super();
	}
	
	public ClosestPoints(GeoPoint point1, GeoPoint point2, double closestDistance) {
		super();
		this.point1 = point1;
		this.point2 = point2;
		this.closestDistance = closestDistance;
	}

	public GeoPoint getPoint1() {
		return point1;
	}

	public void setPoint1(GeoPoint point1) {
		this.point1 = point1;
	}

	public GeoPoint getPoint2() {
		return point2;
	}

	public void setPoint2(GeoPoint point2) {
		this.point2 = point2;
	}

	public double getClosestDistance() {
		return closestDistance;
	}

	public void setClosestDistance(double closestDistance) {
		this.closestDistance = closestDistance;
	}

	@Override
	public String toString() {
		return "ClosestPoints [point1=" + point1.toString() + ", point2=" + point2.toString() + ", closestDistance=" + closestDistance + "]";
	}
	
}

