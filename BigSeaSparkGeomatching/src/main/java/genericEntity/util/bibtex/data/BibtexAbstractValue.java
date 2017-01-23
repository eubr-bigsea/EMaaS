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
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

/**
 * Abstract values are the values that can be used as field values in entries or as bodies of macros.
 * 
 * For example, in
 * 
 * <pre>
 * 	&#x0040;string(pldi = acm # " SIGPLAN Conference of Programming
 * 						Language Design and Implementation")
 * </pre>
 * 
 * the following is an abstract value.
 * 
 * <pre>
 * acm # " SIGPLAN Conference of Programming Language Design and Implementation"
 * </pre>
 * 
 * Other examples:
 * <ul>
 * <li>1971</li>
 * <li>"Johannes Henkel"</li>
 * <li>acm</li>
 * <li>dec</li>
 * </ul>
 * 
 * @author henkel
 */
public abstract class BibtexAbstractValue extends BibtexNode {

	/**
	 * Instantiates a new <code>BibtexAbstractValue</code>.
	 * 
	 * @param bibtexFile
	 *            The file that includes this instance.
	 */
	protected BibtexAbstractValue(BibtexFile bibtexFile) {
		super(bibtexFile);
	}

}
