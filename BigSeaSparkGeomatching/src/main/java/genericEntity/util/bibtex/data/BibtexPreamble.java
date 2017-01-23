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
 * Created on Mar 19, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * A preamble can be used to include pretty much arbitrary LaTex/Tex at the beginning of a generated bibliography. There
 * is usually only one preamble per <code>BibTex</code> file.
 * 
 * @author henkel
 */
public class BibtexPreamble extends BibtexAbstractEntry {

	private BibtexAbstractValue content;

	/**
	 * Instantiates a new <code>BibtexPreamble</code>.
	 * 
	 * @param file
	 *            The file that includes this preamble.
	 * @param content
	 *            The content of this preamble.
	 */
	protected BibtexPreamble(BibtexFile file, BibtexAbstractValue content) {
		super(file);
		this.content = content;
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		writer.println("@preamble{");
		this.content.printBibtex(writer);
		writer.println('}');
	}

	/**
	 * Returns the content of this preamble.
	 * 
	 * @return The content value of this preamble or <code>null</code> if it is not set.
	 */
	public BibtexAbstractValue getContent() {
		return this.content;
	}

	/**
	 * Sets the content.
	 * 
	 * @param content
	 *            The content to set.
	 */
	public void setContent(BibtexAbstractValue content) {
		this.content = content;
	}

}
