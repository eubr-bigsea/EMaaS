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
 * A BibtexString is a literal string, such as "Donald E. Knuth"
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * A String - this class is used for numbers as well - if there's a number wrapped in here, the toString() method will
 * be smart enough to leave out the braces, and thus print {1979} as 1979.
 * 
 * @author henkel
 */
public class BibtexString extends BibtexAbstractValue {

	// content does not include the quotes or curly braces around the string!
	private String content;

	/**
	 * Creates a new <code>BibTexString</code>. The passed String should not be surround be the quotes or curly braces
	 * already.
	 * 
	 * @param file
	 *            The file that includes this String.
	 * @param content
	 *            The content of this <code>BibtexString</code> instance.
	 */
	protected BibtexString(BibtexFile file, String content) {
		super(file);
		this.content = content;
	}

	/**
	 * Returns the content String of this instance (without surrounding quotes or curly braces!).
	 * 
	 * @return The content String of this instance.
	 */
	public String getContent() {
		return this.content;
	}

	/**
	 * Sets the content. The passed String should not be surround be the quotes or curly braces already.
	 * 
	 * @param content
	 *            The content to set.
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * {@inheritDoc BibtexNode#printBibtex(PrintWriter)}
	 * 
	 * Within this method surrounding curly braces will be printed.
	 */
	@Override
	public void printBibtex(PrintWriter writer) {
		// is this really a number?
		try {
			Integer.parseInt(this.content);
			writer.print(this.content);
		} catch (NumberFormatException nfe) {
			writer.print('{');
			// for (int begin = 0; begin < content.length();) {
			// int end = content.indexOf('\n', begin);
			// if (end < 0) {
			// if (begin > 0)
			// writer.print(content.substring(begin, content.length()));
			// else
			// writer.print(content);
			//
			// break;
			// }
			// writer.println(content.substring(begin, end));
			// writer.print("\t\t");
			// begin = end + 1;
			// }
			writer.print(this.content);
			writer.print('}');
		}
	}

}
