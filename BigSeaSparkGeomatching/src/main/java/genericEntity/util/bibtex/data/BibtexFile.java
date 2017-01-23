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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is the root of a <code>BibTex</code> DOM tree and the factory for any <code>BibTex</code> model - the only way
 * to create nodes. For an example, check out the documentation for the constructor of {@link BibtexNode}.
 * 
 * @author henkel
 */
public class BibtexFile extends BibtexNode {

	private final ArrayList<BibtexAbstractEntry> entries = new ArrayList<BibtexAbstractEntry>();

	/**
	 * Instantiates a <code>BibTexFile</code>.
	 */
	public BibtexFile() {
		super(null);
	}

	/**
	 * Adds an entry to this file.
	 * 
	 * @param entry
	 *            The entry that shall be added.
	 */
	public void addEntry(BibtexAbstractEntry entry) {
		this.entries.add(entry);
	}

	/**
	 * Removes the passed entry from this file.
	 * 
	 * @param entry
	 *            The entry that shall be removed.
	 */
	public void removeEntry(BibtexAbstractEntry entry) {
		this.entries.remove(entry);
	}

	/**
	 * Returns an unmodifiable view of the entries.
	 * 
	 * @return An unmodifiable list of the entries.
	 */
	@SuppressWarnings("unchecked")
	public List<BibtexAbstractEntry> getEntries() {
		return Collections.unmodifiableList((List<BibtexAbstractEntry>) this.entries.clone());
	}

	/**
	 * Creates a {@link BibtexConcatenatedValue}. This is simply a factory method.
	 * 
	 * @param left
	 *            The left value of the concatenation.
	 * @param right
	 *            The right value of the concatenation.
	 * @return The concatenated value.
	 */
	public BibtexConcatenatedValue makeConcatenatedValue(BibtexAbstractValue left, BibtexAbstractValue right) {
		return new BibtexConcatenatedValue(this, left, right);
	}

	/**
	 * Creates a {@link BibtexEntry}. This is simply a factory method.
	 * 
	 * @param entryType
	 *            The type of the entry.
	 * @param entryKey
	 *            The key of the entry.
	 * @return The <code>BibTex</code> entry.
	 */
	public BibtexEntry makeEntry(String entryType, String entryKey) {
		return new BibtexEntry(this, entryType, entryKey);
	}

	/**
	 * Creates a {@link BibtexPersonList}. This is simply a factory method.
	 * 
	 * @return The person list.
	 */
	public BibtexPersonList makePersonList() {
		return new BibtexPersonList(this);
	}

	/**
	 * Creates a {@link BibtexPerson}. This is simply a factory method.
	 * 
	 * @param first
	 *            The first name of the person.
	 * @param preLast
	 *            Any middle name or initials.
	 * @param last
	 *            The last name.
	 * @param lineage
	 *            The lineage of the person.
	 * @param isOthers
	 *            <code>true</code>, if the created <code>BibTexPerson</code> represents the <code>BibTex</code>
	 *            "Other authors" value.
	 * @return The <code>BibTex</code> person.
	 */
	public BibtexPerson makePerson(String first, String preLast, String last, String lineage, boolean isOthers) {
		return new BibtexPerson(this, first, preLast, last, lineage, isOthers);
	}

	/**
	 * Creates a {@link BibtexPreamble}. This is simply a factory method.
	 * 
	 * @param content
	 *            The content of the preamble.
	 * @return The <code>BibTex</code> preamble.
	 */
	public BibtexPreamble makePreamble(BibtexAbstractValue content) {
		return new BibtexPreamble(this, content);
	}

	/**
	 * Creates a {@link BibtexString}. This is simply a factory method. <code>content</code> should not include the
	 * quotes or curly braces around the String.
	 * 
	 * @param content
	 *            The content of the String.
	 * @return The <code>BibTex</code> String.
	 */
	public BibtexString makeString(String content) {
		return new BibtexString(this, content);
	}

	/**
	 * Creates a {@link BibtexMacroDefinition}. This is simply a factory method.
	 * 
	 * @param key
	 *            The key of the macro.
	 * @param value
	 *            The value of the macro.
	 * @return The <code>BibTex</code> macro definition.
	 */
	public BibtexMacroDefinition makeMacroDefinition(String key, BibtexAbstractValue value) {
		return new BibtexMacroDefinition(this, key, value);
	}

	/**
	 * Creates a {@link BibtexMacroReference}. This is simply a factory method.
	 * 
	 * @param key
	 *            The key of the macro reference.
	 * @return The <code>BibTex</code> macro reference.
	 */
	public BibtexMacroReference makeMacroReference(String key) {
		return new BibtexMacroReference(this, key);
	}

	/**
	 * Creates a {@link BibtexToplevelComment}. This is simply a factory method.
	 * 
	 * @param content
	 *            The content of the comment.
	 * @return The <code>BibTex</code> top-level comment.
	 */
	public BibtexToplevelComment makeToplevelComment(String content) {
		return new BibtexToplevelComment(this, content);
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		for (Iterator<BibtexAbstractEntry> iter = this.entries.iterator(); iter.hasNext();) {
			BibtexNode node = iter.next();
			node.printBibtex(writer);
		}
		writer.flush();
	}

}
