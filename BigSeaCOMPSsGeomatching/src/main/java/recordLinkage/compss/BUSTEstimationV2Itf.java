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
import recordLinkage.dependencies.TicketInformation;
import scala.Tuple3;

public interface BUSTEstimationV2Itf {	
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")
	public HashMap<String, LinkedList<ShapePoint>> mapShape(
			@Parameter(type = Type.FILE, direction = Direction.IN)String shapeSource
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")
	public HashMap<String, ShapeLine> groupShape(
			@Parameter HashMap<String, LinkedList<ShapePoint>> shapePair
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")
	public HashMap<String, HashMap<String, String>> mapBusStops(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")
	public HashMap<String, LinkedList<TicketInformation>> mapTicketsSplitted(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")
	public HashMap<String, LinkedList<BulmaOutput>> mapBulmaOutputSplitted(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")	
	public LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergeInputs(
			@Parameter HashMap<String, LinkedList<BulmaOutput>> partialBulmaOutput, 
			@Parameter HashMap<String, ShapeLine> groupedShape,
			@Parameter HashMap<String, HashMap<String, String>> mapStopPoints
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")	
	public HashMap<String, LinkedList<String>> generateTmpOutput(
			@Parameter LinkedList<Tuple3<BulmaOutputGrouping, ShapeLine, HashMap<String, String>>> mergedOutput
	);
	
	@Method(declaringClass = "recordLinkage.compss.BUSTEstimationV2")	
	public LinkedList<String> insertTicketsInformation (
			@Parameter HashMap<String, LinkedList<String>> listOutput, 
			@Parameter HashMap<String, LinkedList<TicketInformation>> tickets, 
			@Parameter LinkedList<String> results
	);
}
