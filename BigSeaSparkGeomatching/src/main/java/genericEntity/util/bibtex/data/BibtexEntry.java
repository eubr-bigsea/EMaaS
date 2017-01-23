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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An entry is something that can be referenced by a latex file using the \cite command. E.g.
 * 
 * <pre>
 * &#x0040inproceedings{diwan98typebased,
 *    year=1998,
 *    pages={106-117},
 *    title={Type-Based Alias Analysis},
 *    url={citeseer.nj.nec.com/diwan98typebased.html},
 *    booktitle={SIGPLAN Conference on Programming Language Design and Implementation},
 *    author={Amer Diwan and Kathryn S. McKinley and J. Eliot B. Moss},
 * }
 * </pre>
 * 
 * For an example of how to create BibtexEntry objects, check out the documentation for the constructor of BibtexNode.
 * 
 * @author henkel
 */
public class BibtexEntry extends BibtexAbstractEntry {

	/**
	 * Instantiates a new <code>BibtexEntry</code>.
	 * 
	 * @param file
	 *            The file that includes this entry.
	 * @param entryType
	 *            The type of the entry.
	 * @param entryKey
	 *            The key of the entry.
	 */
	protected BibtexEntry(BibtexFile file, String entryType, String entryKey) {
		super(file);
		this.entryKey = entryKey;
		// we intern the entry type for space optimization.
		this.entryType = entryType.toLowerCase().intern();
	}

	private String entryType;
	private String entryKey;
	private HashMap<String, BibtexAbstractValue> fields = new HashMap<String, BibtexAbstractValue>();

	/**
	 * Returns the key of this entry.
	 * 
	 * @return The key of this entry or <code>null</code> if it is not set.
	 */
	public String getEntryKey() {
		return this.entryKey;
	}

	/**
	 * Returns the value of field specified by the passed field name.
	 * 
	 * @param name
	 *            The name of the field whose value is requested.
	 * @return The field value of the requested field or <code>null</code> if it is not set.
	 */
	public BibtexAbstractValue getFieldValue(String name) {
		return this.fields.get(name);
	}

	/**
	 * Returns the type of this entry.
	 * 
	 * @return The type of this entry or <code>null</code> if it is not set.
	 */
	public String getEntryType() {
		return this.entryType;
	}

	/**
	 * Returns a read only view of the field map. This is a mapping from String instances (field names) to instances of
	 * <code>BibtexAbstractValue</code>.
	 * 
	 * @return The field-value mapping.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, BibtexAbstractValue> getFields() {
		return Collections.unmodifiableMap((Map<String, BibtexAbstractValue>) this.fields.clone());
	}

	/**
	 * Sets a field within this entry.
	 * 
	 * @param fieldName
	 *            The name of the field.
	 * @param fieldValue
	 *            The value of the field.
	 */
	public void setField(String fieldName, BibtexAbstractValue fieldValue) {
		// we intern fieldName for space optimization.
		this.fields.put(fieldName.toLowerCase().intern(), fieldValue);
	}

	/**
	 * Sets the entryKey.
	 * 
	 * @param entryKey
	 *            The entryKey to set
	 */
	public void setEntryKey(String entryKey) {
		assert entryKey != null : "BibtexEntry.setEntryKey(String entryKey): encountered entryKey==null.";
		this.entryKey = entryKey.toLowerCase();
	}

	/**
	 * Sets the entryType.
	 * 
	 * @param entryType
	 *            The entryType to set
	 */
	public void setEntryType(String entryType) {
		assert entryType != null : "BibtexEntry.setEntryType(String entryType): encountered entryType==null";
		this.entryType = entryType.toLowerCase().intern();
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		writer.print('@');
		writer.print(this.entryType);
		writer.print('{');
		writer.print(this.entryKey);
		writer.println(',');
		String keys[] = new String[this.fields.keySet().size()];
		this.fields.keySet().toArray(keys);
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			BibtexNode value = this.fields.get(key);
			writer.print('\t');
			writer.print(key);
			writer.print('=');
			value.printBibtex(writer);
			writer.println(',');
		}

		writer.println('}');
	}

	/**
	 * Removes the field from this entry.
	 * 
	 * @param fieldName
	 *            The name of the field that shall be removed.
	 */
	public void undefineField(String fieldName) {
		this.fields.remove(fieldName);
	}

}
