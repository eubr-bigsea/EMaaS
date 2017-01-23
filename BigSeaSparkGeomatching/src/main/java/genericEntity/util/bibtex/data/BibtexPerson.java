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
 * Created on Mar 27, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * <code>BibtexPerson</code> objects are elements of {@link BibtexPersonList}s, which can be used in author or editor
 * fields.
 * 
 * E.g. Charles Louis Xavier Joseph de la Vall{'e}e Poussin Jr:
 * 
 * <pre>
 * first = "Charles Louis Xavier Joseph"
 * preLast = "de la"
 * last = "Vall{'e}e Poussin"
 * lineage = "Jr"
 * </pre>
 * 
 * Fields that are not used are set to <code>null</code>. If <code>isAndOthers</code> is <code>true</code>, all fields
 * are ignored (should be <code>null</code>).
 * 
 * @author henkel
 */
public final class BibtexPerson extends BibtexNode {

	/**
	 * Instantiates a new <code>BibtexPerson</code>.
	 * 
	 * @param file
	 *            The file that includes this person.
	 * @param first
	 *            The first name of the person.
	 * @param preLast
	 *            The middle name or any initials of this person.
	 * @param last
	 *            The last name of this person.
	 * @param lineage
	 *            The lineage of this person.
	 * @param isOthers
	 *            <code>true</code>, if the created <code>BibTexPerson</code> represents the <code>BibTex</code>
	 *            "Other authors" value.
	 */
	protected BibtexPerson(BibtexFile file, String first, String preLast, String last, String lineage, boolean isOthers) {
		super(file);
		this.first = first;
		this.preLast = preLast;
		this.last = last;
		this.lineage = lineage;
		this.isOthers = isOthers;
	}

	private String first, preLast, last, lineage;
	private boolean isOthers;

	/**
	 * Returns the first name of the person.
	 * 
	 * @return The first name of the person or <code>null</code> if it is not set.
	 */
	public String getFirst() {
		return this.first;
	}

	/**
	 * Returns <code>true</code>, if this instance represents the <code>BibTex</code> "Other authors" value; otherwise
	 * <code>false</code>.
	 * 
	 * @return <code>true</code>, if this instance represents the <code>BibTex</code> "Other authors" value; otherwise
	 *         <code>false</code>.
	 */
	public boolean isOthers() {
		return this.isOthers;
	}

	/**
	 * Returns the last name of the person.
	 * 
	 * @return The last name of the person or <code>null</code> if it is not set.
	 */
	public String getLast() {
		return this.last;
	}

	/**
	 * Returns the lineage of this person.
	 * 
	 * @return The lineage of this person or <code>null</code> if it is not set.
	 */
	public String getLineage() {
		return this.lineage;
	}

	/**
	 * Returns the middle name or any middle initials of this persons.
	 * 
	 * @return The middle name or any middle initials of this persons or <code>null</code> if it is not set.
	 */
	public String getPreLast() {
		return this.preLast;
	}

	/**
	 * Sets the first.
	 * 
	 * @param first
	 *            The first to set.
	 */
	public void setFirst(String first) {
		this.first = first;
	}

	/**
	 * Sets the isAndOthers.
	 * 
	 * @param isAndOthers
	 *            The isAndOthers to set.
	 */
	public void setOthers(boolean isAndOthers) {
		this.isOthers = isAndOthers;
	}

	/**
	 * Sets the last.
	 * 
	 * @param last
	 *            The last to set.
	 */
	public void setLast(String last) {
		this.last = last;
	}

	/**
	 * Sets the lineage.
	 * 
	 * @param lineage
	 *            The lineage to set.
	 */
	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	/**
	 * Sets the preLast.
	 * 
	 * @param preLast
	 *            The preLast to set.
	 */
	public void setPreLast(String preLast) {
		this.preLast = preLast;
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		if (this.isOthers) {
			writer.print("others");
		} else {
			if (this.preLast != null) {
				writer.print(this.preLast);
				writer.print(' ');
			}
			writer.print(this.last);
			if (this.lineage != null && this.first != null)
				writer.print(", ");
			if (this.lineage != null) {
				writer.print(this.lineage);
				if (this.first != null)
					writer.print(", ");
			}
			if (this.first != null) {
				writer.print(this.first);
			}
		}
	}

}
