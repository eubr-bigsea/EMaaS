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
 * Created on Mar 29, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package genericEntity.util.bibtex.expander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import genericEntity.util.bibtex.data.BibtexAbstractEntry;
import genericEntity.util.bibtex.data.BibtexAbstractValue;
import genericEntity.util.bibtex.data.BibtexEntry;
import genericEntity.util.bibtex.data.BibtexFile;
import genericEntity.util.bibtex.data.BibtexString;

/**
 * This expander expands the crossreferences defined by the crossref fields -
 * you should run the MacroReferenceExpander first.
 * 
 * @author henkel
 */
public final class CrossReferenceExpander extends AbstractExpander implements Expander {

	/** Equivalent to CrossReferenceExpander(true) */
	public CrossReferenceExpander() {
		this(true);
	}
	
	/**
	 * @param throwAllExpansionExceptions
	 *            Setting this to true means that all exceptions will be thrown
	 *            immediately. Otherwise, the expander will skip over things it
	 *            can't expand and you can use getExceptions to retrieve the
	 *            exceptions later
	 */
	public CrossReferenceExpander(boolean throwAllExpansionExceptions) {
		super(throwAllExpansionExceptions);
	}

	/**
	 * Note: If you don't use the MacroReferenceExpander first, this function
	 * may lead to inconsistent macro references.
	 * 
	 * If you use the flag throwAllExpansionExceptions set to false, you can
	 * retrieve all the exceptions using getExceptions()
	 * 
	 * @param bibtexFile
	 */
	public void expand(BibtexFile bibtexFile) throws ExpansionException {
		HashMap<String, BibtexAbstractEntry> entryKey2Entry = new HashMap<String, BibtexAbstractEntry>();
		ArrayList<BibtexEntry> entriesWithCrossReference = new ArrayList<BibtexEntry>();
		for (Iterator<BibtexAbstractEntry> entryIt = bibtexFile.getEntries().iterator(); entryIt.hasNext();) {
			BibtexAbstractEntry abstractEntry = entryIt.next();
			if (!(abstractEntry instanceof BibtexEntry)) {
				continue;
			}
			BibtexEntry entry = (BibtexEntry) abstractEntry;
			entryKey2Entry.put(entry.getEntryKey().toLowerCase(), abstractEntry);
			if (entry.getFields().containsKey("crossref")) {
				entriesWithCrossReference.add(entry);
			}
		}
		for (Iterator<BibtexEntry> entryIt = entriesWithCrossReference.iterator(); entryIt.hasNext();) {
			BibtexEntry entry = entryIt.next();
			String crossrefKey = ((BibtexString) entry.getFields().get("crossref")).getContent().toLowerCase();
			entry.undefineField("crossref");
			BibtexEntry crossrefEntry = (BibtexEntry) entryKey2Entry.get(crossrefKey);
			if (crossrefEntry == null) {
				throwExpansionException("Crossref key not found: \"" + crossrefKey + "\"");
				continue;
			}
			if (crossrefEntry.getFields().containsKey("crossref"))
				throwExpansionException(
					"Nested crossref: \""
						+ crossrefKey
						+ "\" is crossreferenced but crossreferences itself \""
						+ ((BibtexString) crossrefEntry.getFields().get("crossref")).getContent()
						+ "\"");
			Map<String, BibtexAbstractValue> entryFields = entry.getFields();
			Map<String, BibtexAbstractValue> crossrefFields = crossrefEntry.getFields();
			for (Iterator<String> fieldIt = crossrefFields.keySet().iterator(); fieldIt.hasNext();) {
				String key = fieldIt.next();
				if (!entryFields.containsKey(key)) {
					entry.setField(key, crossrefFields.get(key));
				}
			}
		}
		finishExpansion();
	}
}
