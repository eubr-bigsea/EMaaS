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
 * Created on Mar 27, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of {@link BibtexPerson} objects that can be used for author or editor fields - use the PersonListExpander to
 * convert all editor/author field values of a particular {@link BibtexFile} to <code>BibtexPersonLists</code>.
 * 
 * @author henkel
 */
public final class BibtexPersonList extends BibtexAbstractValue {

	/**
	 * Instantiates a new <code>BibtexPersonList</code>.
	 * 
	 * @param file
	 *            The file that includes this list.
	 */
	protected BibtexPersonList(BibtexFile file) {
		super(file);
	}

	private LinkedList<BibtexPerson> list = new LinkedList<BibtexPerson>();

	/**
	 * Returns a read-only list whose members are instances of {@link BibtexPerson}.
	 * 
	 * @return A unmodifiable {@link List} representation of this instance.
	 */
	public List<BibtexPerson> getList() {
		return Collections.unmodifiableList(this.list);
	}

	/**
	 * Adds a {@link BibtexPerson} to this list.
	 * 
	 * @param bibtexPerson
	 *            The <code>BibtexPerson</code> instance that shall be added.
	 */
	public void add(BibtexPerson bibtexPerson) {
		this.list.add(bibtexPerson);
	}

	@Override
	public void printBibtex(PrintWriter writer) {
		boolean isFirst = true;
		writer.print('{');
		for (Iterator<BibtexPerson> it = this.list.iterator(); it.hasNext();) {
			if (isFirst) {
				isFirst = false;
			} else
				writer.print(" and ");
			(it.next()).printBibtex(writer);
		}
		writer.print('}');
	}

}
