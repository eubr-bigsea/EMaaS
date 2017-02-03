package LineDependencies;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

public class ShapesUnion {

	private static List<ShapeLine> shapesList;
	private int MAX;
	private int N;

	public ShapesUnion(List<ShapeLine> entrada) {
		this.shapesList = entrada;
		this.MAX = ~(1 << entrada.size());
		this.N = 1;
	}

	/**
	 * @return true if there is at least one combination available
	 */
	private boolean hasNext() {
		return (this.N & this.MAX) != 0;
	}

	/**
	 * @return the number of active bits (= 1)
	 */
	private int countbits() {
		int i = 1;
		int activeBits = 0;
		while ((this.MAX & i) != 0) {
			if ((this.N & i) != 0) {
				activeBits++;
			}
			i = i << 1;
		}

		return activeBits;
	}

	/**
	 * @return the size of the output
	 */
	private int getSaidaLength() {
		return this.countbits();
	}

	/**
	 * @return the next combination
	 */
	private List<GeoLine> next() {
		List<GeoLine> output = new ArrayList<>();
		int indexInput = 0;
		int i = 1;

		while ((this.MAX & i) != 0) {
			if ((this.N & i) != 0) {
				output.add(shapesList.get(indexInput));
			}
			indexInput += 1;
			i = i << 1;
		}

		N += 1;

		return output;
	}

	/**
	 * 
	 * @return all the possibles unions generated from the input list
	 */
	public List<Geometry> generateAllUnions() {
		List<GeoLine> nextCombination;
		List<Geometry> unionsList = new ArrayList<>();
		Geometry geometry = null;

		while (this.hasNext()) {
			nextCombination = this.next();
			for (int i = 0; i < nextCombination.size(); i++) {

				if (i == 0) {
					geometry = nextCombination.get(i).getLine();
				} else {
					geometry = geometry.union(nextCombination.get(i).getLine());
				}
			}
			unionsList.add(geometry);
		}

		return unionsList;
	}
}