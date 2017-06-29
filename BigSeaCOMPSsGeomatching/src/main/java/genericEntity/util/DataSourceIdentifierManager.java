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

package genericEntity.util;

import java.util.Collection;
import java.util.HashSet;

import genericEntity.datasource.DataSource;
import genericEntity.exception.IdentifierIsAlreadyInUseException;

/**
 * <code>IdentifierManager</code> manages the {@link DataSource} identifiers. It is implemented as a Singleton.
 * 
 * @author Ziawasch Abedjan
 */
public class DataSourceIdentifierManager {

	private static DataSourceIdentifierManager instance = new DataSourceIdentifierManager();

	private Collection<String> usedIdentifiers = new HashSet<String>();

	/**
	 * Default constructor that can only be called within the class.
	 */
	private DataSourceIdentifierManager() {
		// nothing to do - it's only set in order to make the default
		// constructor private
	}

	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return The instance of <code>IdentifierManager</code>.
	 */
	public static DataSourceIdentifierManager getInstance() {
		return DataSourceIdentifierManager.instance;
	}

	/**
	 * Checks whether the passed id is already in use.
	 * 
	 * @param id
	 *            The identifier that shall be checked.
	 * @return <code>true</code>, if the id is already in use; otherwise <code>false</code>.
	 */
	public boolean isAlreadyInUse(String id) {
		return this.usedIdentifiers.contains(id);
	}

	/**
	 * Adds the id to the set of identifiers that is already in use.
	 * 
	 * @param id
	 *            The identifier that shall be added.
	 * 
	 * @throws IdentifierIsAlreadyInUseException
	 *             If the passed id is already registered. The reason for this might be that the identifier is already in use by another
	 *             {@link DataSource}.
	 */
	public void add(String id) {
		if (this.isAlreadyInUse(id)) {
			throw new IdentifierIsAlreadyInUseException("The passed DataSource identifier '" + id
					+ "' is used more than one time. Please choose another identifier.");
		}

		this.usedIdentifiers.add(id);
	}

	/**
	 * Removes the passed id from the set of already used identifiers.
	 * 
	 * @param id
	 *            The id that shall be removed.
	 */
	public void remove(String id) {
		this.usedIdentifiers.remove(id);
	}

}
