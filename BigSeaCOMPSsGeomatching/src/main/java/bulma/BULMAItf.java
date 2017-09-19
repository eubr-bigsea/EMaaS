package bulma;

import java.util.HashMap;
import java.util.LinkedList;

import bulma.dependencies.GPSLine;
import bulma.dependencies.GeoPoint;
import bulma.dependencies.ShapeLine;
import integratedtoolkit.types.annotations.Parameter;
import integratedtoolkit.types.annotations.parameter.Direction;
import integratedtoolkit.types.annotations.parameter.Type;
import integratedtoolkit.types.annotations.task.Method;

public interface BULMAItf {

	@Method(declaringClass = "bulma.BULMA")
	public HashMap<String, LinkedList<GeoPoint>> mapShape(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath);

	@Method(declaringClass = "bulma.BULMA")
	public HashMap<String, LinkedList<ShapeLine>> groupShape(
			@Parameter HashMap<String, LinkedList<GeoPoint>> shapePair);

	@Method(declaringClass = "bulma.BULMA")
	public HashMap<String, LinkedList<GeoPoint>> mapGPSFile(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath);

	@Method(declaringClass = "bulma.BULMA")
	public HashMap<String, LinkedList<GPSLine>> groupGPSFile(
			@Parameter HashMap<String, LinkedList<GeoPoint>> mapGPSPoints);

	@Method(declaringClass = "bulma.BULMA")
	public LinkedList<GPSLine> mapPossibleShapes(
			@Parameter HashMap<String, LinkedList<GPSLine>> groupedGPS,
			@Parameter HashMap<String, LinkedList<ShapeLine>> groupedShape);
	
	@Method(declaringClass = "bulma.BULMA")
	public LinkedList<GPSLine> getTrueShapes(
			@Parameter LinkedList<GPSLine> gpsPlusPossibleShapes);
	
	@Method(declaringClass = "bulma.BULMA")
	public LinkedList<GPSLine> getClosestPoints(
			@Parameter LinkedList<GPSLine> gpsPlusTrueShapes);
	
	@Method(declaringClass = "bulma.BULMA")
	public LinkedList<String> generateOutput(
			@Parameter LinkedList<GPSLine> closestPoints, 
			@Parameter LinkedList<String> results);
}
