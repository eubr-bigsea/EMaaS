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
package genericEntity.util.bibtex.parser;

/**
 * This exception is raised if an error occurs while parsing using the {@link BibtexParser}.
 * 
 * @author henkel
 */
@SuppressWarnings("serial")
public final class ParseException extends Exception {

	/**
	 * Initializes a <code>ParseException</code>.
	 * 
	 * @param line
	 *            The line in which the parsing error occurred.
	 * @param column
	 *            The column in which the parsing error occurred.
	 * @param encountered
	 *            The String that was read.
	 * @param expected
	 *            The String that was actually expected.
	 */
	ParseException(int line, int column, String encountered, String expected) {
		super("" + line + ":" + column + ": encountered '" + encountered + "', expected '" + expected + "'.");
	}

}
