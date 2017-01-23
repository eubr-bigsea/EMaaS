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
 * A BibtexMacroReference references a BibtexMacroDefinition
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * A BibtexMacroReference references a BibtexMacroDefinition.
 * 
 * @author henkel
 */
public class BibtexMacroReference extends BibtexAbstractValue {

	/**
	 * Instantiates a new {@link BibtexMacroReference}.
	 * 
	 * @param file
	 *            The file that includes this macro reference.
	 * @param key
	 *            The key of the reference.
	 */
	protected BibtexMacroReference(BibtexFile file, String key) {
		super(file);
		this.key = key.toLowerCase();
	}

	private String key;

	/**
	 * Returns the key of this reference.
	 * 
	 * @return The key of this reference or <code>null</code> if it is not set.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Sets the key.
	 * 
	 * @param key
	 *            The key to set.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		writer.print(this.key);
	}

}
