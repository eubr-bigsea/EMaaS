package recordLinkage.compss;

import java.util.HashMap;
import java.util.LinkedList;

import PointDependencies.ShapePoint;
import integratedtoolkit.types.annotations.Parameter;
import integratedtoolkit.types.annotations.parameter.Direction;
import integratedtoolkit.types.annotations.parameter.Type;
import integratedtoolkit.types.annotations.task.Method;
import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.BulmaOutputGrouping;
import recordLinkage.dependencies.ShapeLine;
import scala.Tuple3;

public interface BUSTEstimationItf {
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, LinkedList<ShapePoint>> mapShape(
		@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, HashMap<String, String>> mapBusStops(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
 	public HashMap<String, LinkedList<BulmaOutput>> mapBulmaOutput(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath	
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public LinkedList<String> generateOutput(
			@Parameter LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergedOutput, 
			@Parameter LinkedList<String> results
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, ShapeLine> groupShape(
			@Parameter HashMap<String, LinkedList<ShapePoint>> shapePair
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")	
	public LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergeInputs(
			@Parameter HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput, 
			@Parameter HashMap<String, ShapeLine> groupedShape,
			@Parameter HashMap<String, HashMap<String, String>> mapStopPoints
	);
}
