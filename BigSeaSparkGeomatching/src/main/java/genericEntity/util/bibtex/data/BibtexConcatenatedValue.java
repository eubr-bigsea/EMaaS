/*
 * GenericObject - The Duplicate Detection Toolkit
 * 
 * Copyright (C) 2010  Hasso-Plattner-Institut für Softwaresystemtechnik GmbH,
 *                     Potsdam, Germany 
 *
 * This file is part of GenericObject.
 * 
 * GenericObject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GenericObject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GenericObject.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

/*
 * E. g. "asdf " # 19
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * Two abstract values concatenated by the hash-operator (#).
 * 
 * Examples:
 * <ul>
 * <li>acm # " SIGPLAN"</li>
 * <li>"10th " # pldi</li>
 * </ul>
 * 
 * @author henkel
 */
public class BibtexConcatenatedValue extends BibtexAbstractValue {

	/**
	 * Instantiates a new <code>BibtexConcatenatedValue</code> using the specified values.
	 * 
	 * @param file
	 *            The file that includes this concatenation.
	 * @param left
	 *            The left value of the concatenated value.
	 * @param right
	 *            The right value of the concatenated value.
	 */
	protected BibtexConcatenatedValue(BibtexFile file, BibtexAbstractValue left, BibtexAbstractValue right) {
		super(file);
		this.left = left;
		this.right = right;
	}

	private BibtexAbstractValue left, right;

	/**
	 * Returns the left value of this concatenation.
	 * 
	 * @return The left value or <code>null</code> if it is not set.
	 */
	public BibtexAbstractValue getLeft() {
		return this.left;
	}

	/**
	 * Returns the right value of this concatenation.
	 * 
	 * @return The right value or <code>null</code> if it is not set.
	 */
	public BibtexAbstractValue getRight() {
		return this.right;
	}

	/**
	 * Sets the left.
	 * 
	 * @param left
	 *            The left to set.
	 */
	public void setLeft(BibtexAbstractValue left) {
		this.left = left;
	}

	/**
	 * Sets the right.
	 * 
	 * @param right
	 *            The right to set.
	 */
	public void setRight(BibtexAbstractValue right) {
		this.right = right;
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		this.left.printBibtex(writer);
		writer.print('#');
		this.right.printBibtex(writer);
	}

}
