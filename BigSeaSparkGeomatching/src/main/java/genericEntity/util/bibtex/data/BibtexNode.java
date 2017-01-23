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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An abstract superclass for all <code>BibTex</code> model nodes.
 * 
 * @author henkel
 */
public abstract class BibtexNode {

	/**
	 * Some people have asked why this constructor has a parameter of type {@link BibtexFile} (which in turn extends
	 * <code>BibtexNode</code>). The reason is that each BibtexNode has a reference to the <code>BibtexFile</code> which
	 * it belongs to. Of course, for the file itself that pointer is null.
	 * 
	 * By the way, it's unlikely that you need to call this constructor or any constructor for <code>BibtexNodes</code>
	 * other than <code>BibtexFile</code> directly - instead, use the <code>BibtexFile</code> as a factory.
	 * 
	 * For example, to create a <code>BibTex</code> file equivalent of this
	 * 
	 * <pre>
	 * ================
	 * &#64;article{test1,
	 * author="Johannes Henkel",
	 * title="README"
	 * }
	 * ================
	 * you'd do
	 * 
	 * BibtexFile bibtexFile = new BibtexFile();
	 * BibtexEntry onlyEntry = bibtexFile.makeEntry("article","test1");
	 * onlyEntry.setField("author",bibtexFile.makeString("Johannes Henkel"));
	 * onlyEntry.setField("title",bibtexFile.makeString("README"));
	 * </pre>
	 * 
	 * @param bibtexFile
	 */
	protected BibtexNode(BibtexFile bibtexFile) {
		this.bibtexFile = bibtexFile;
	}

	private final BibtexFile bibtexFile;

	/**
	 * Returns the owner file of this node.
	 * 
	 * @return The owner file of this node or <code>null</code> if it is not set.
	 */
	public BibtexFile getOwnerFile() {
		return this.bibtexFile;
	}

	/**
	 * Prints the node to the passed <code>PrintWriter</code>. This method needs to be implemented by each sub-class.
	 * 
	 * @param writer
	 *            The writer that shall be used for writing the String representation of this node.
	 */
	public abstract void printBibtex(PrintWriter writer);

	@Override
	public String toString() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		printBibtex(out);
		out.flush();
		return stringWriter.toString();
	}

}
