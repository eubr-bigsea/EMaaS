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
 * A string definition is something like
 * 
 * @string{ cacm = "Communications of the ACM }
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * <code>BibTex</code> let's you define macros which are essentially just shortcuts for strings. Macros can reference
 * other macros, as long as there's no cycle.
 * 
 * Examples:
 * <ul>
 * <li>&#x0040string(acm = "Association of the Computing Machinery")</li>
 * <li>&#x0040string(acmsigplan = acm # " SIGPLAN")</li>
 * </ul>
 * 
 * @author henkel
 */
public class BibtexMacroDefinition extends BibtexAbstractEntry {

	/**
	 * Instantiates a new <code>BibtexMacroDefinition</code>.
	 * 
	 * @param file
	 *            The file that contains this macro definition.
	 * @param key
	 *            The key of the macro definition.
	 * @param value
	 *            the value of the macro definition.
	 */
	protected BibtexMacroDefinition(BibtexFile file, String key, BibtexAbstractValue value) {
		super(file);
		this.key = key.toLowerCase();
		this.value = value;
	}

	private String key;
	private BibtexAbstractValue value;

	/**
	 * Returns the key of the macro definition.
	 * 
	 * @return The key of the macro definition or <code>null</code> if it is not set.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Returns the value of the macro definition.
	 * 
	 * @return The value of the macro definition or <code>null</code> if it is not set.
	 */
	public BibtexAbstractValue getValue() {
		return this.value;
	}

	/**
	 * Sets the key.
	 * 
	 * @param key
	 *            The key to set.
	 */
	public void setKey(String key) {
		this.key = key.toLowerCase();
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            The value to set.
	 */
	public void setValue(BibtexAbstractValue value) {
		this.value = value;
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		writer.print("@string{");
		writer.print(this.key);
		writer.print('=');
		this.value.printBibtex(writer);
		writer.println('}');

	}

}
