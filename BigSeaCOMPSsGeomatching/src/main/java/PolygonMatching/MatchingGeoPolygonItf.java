package PolygonMatching;

import integratedtoolkit.types.annotations.Parameter;
import integratedtoolkit.types.annotations.parameter.Direction;
import integratedtoolkit.types.annotations.parameter.Type;
import integratedtoolkit.types.annotations.task.Method;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;

import PolygonDependencies.PolygonPair;

public interface MatchingGeoPolygonItf {

	@Method(declaringClass = "SingleMatchingGeoPolygon.MatchingGeoPolygon")
	public ArrayList<Tuple2<Integer, PolygonPair>> mergeResults(
			@Parameter ArrayList<Tuple2<Integer, PolygonPair>> m1,
			@Parameter ArrayList<Tuple2<Integer, PolygonPair>> m2
	);

	@Method(declaringClass = "SingleMatchingGeoPolygon.MatchingGeoPolygon")
	public HashMap<Integer, PolygonPair> map(
			@Parameter(type = Type.FILE, direction = Direction.IN) String filePath
	);
}
