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
 * top-level comments are comments starting with %
 * For simplicity, we allow such comments only outside of all other entries.
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;

/**
 * Top-level comments wrap everything that is at the top-level of a <code>BibTex</code> file and not parsable as some
 * other entry. Note that many people think that the <code>LaTeX</code> comment symbol '%' can be used in
 * <code>BibTex</code>. That's a myth - <code>BibTex</code> will just ignore that. If you want to comment out in
 * <code>BibTex</code>, remove the '@' sign at the beginning of an entry.
 * 
 * @author henkel
 */
public class BibtexToplevelComment extends BibtexAbstractEntry {

	/**
	 * Creates a new <code>BibtexToplevelComment</code>.
	 * 
	 * @param file
	 *            The file that includes this comment.
	 * @param content
	 *            The content of this comment.
	 */
	protected BibtexToplevelComment(BibtexFile file, String content) {
		super(file);
		this.content = content;
	}

	private String content;

	/**
	 * Returns the content of this comment.
	 * 
	 * @return The content of this comment or <code>null</code> if it is not set.
	 */
	public String getContent() {
		return this.content;
	}

	/**
	 * Sets the content.
	 * 
	 * @param content
	 *            The content to set.
	 */
	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		writer.println(this.content);
	}

}
