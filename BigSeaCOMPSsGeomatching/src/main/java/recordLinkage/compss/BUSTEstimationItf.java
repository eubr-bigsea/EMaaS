package recordLinkage.compss;

import java.util.HashMap;
import java.util.LinkedList;

import org.junit.runners.Parameterized.Parameters;

import PointDependencies.ShapePoint;
import integratedtoolkit.types.annotations.Parameter;
import integratedtoolkit.types.annotations.parameter.Direction;
import integratedtoolkit.types.annotations.parameter.Type;
import integratedtoolkit.types.annotations.task.Method;
import recordLinkage.dependencies.BulmaOutput;
import recordLinkage.dependencies.BulmaOutputGrouping;
import recordLinkage.dependencies.ShapeLine;

public interface BUSTEstimationItf {
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public  HashMap<String, LinkedList<ShapePoint>> mapShape(
		@Parameter(type = Type.FILE, direction = Direction.IN)String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, String> mapBusStops(
		@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, LinkedList<BulmaOutput>> mapBulmaOutput(
		@Parameter(type = Type.FILE, direction = Direction.IN)String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public LinkedList<String> mergeResults(
			@Parameter HashMap<String, BulmaOutputGrouping> groupedOutput,
			@Parameter HashMap<String, ShapeLine> groupedShape,
			@Parameter HashMap<String, String> mapStopPoints,
			@Parameter LinkedList<String> results
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, ShapeLine> groupShape(
			@Parameter HashMap<String, LinkedList<ShapePoint>> shapePair
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimation")
	public HashMap<String, BulmaOutputGrouping> groupBulmaOutput(
			@Parameter HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput
	);
	
}
