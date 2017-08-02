package recordLinkage.dependencies;

import java.io.Serializable;
import java.util.List;

import PointDependencies.ShapePoint;

public class ShapeLine implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String shapeId;
	private List<ShapePoint> listGeoPoint;
	private String route;
	
	public ShapeLine(String shapeId, List<ShapePoint> listGeoPoint, String route) {
		this.shapeId = shapeId;
		this.listGeoPoint = listGeoPoint;
		this.route = route;
	}
	
	public String getShapeId() {
		return shapeId;
	}
	
	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}
	
	public List<ShapePoint> getListGeoPoint() {
		return listGeoPoint;
	}
	
	public void setListGeoPoint(List<ShapePoint> listGeoPoint) {
		this.listGeoPoint = listGeoPoint;
	}
	
	public String getRoute() {
		return route;
	}
	
	public void setRoute(String route) {
		this.route = route;
	}
}
