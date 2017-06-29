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

package genericEntity.database.util;

import genericEntity.util.data.AutoJsonable;


/**
 * <code>ColumnInfo</code> represents a column with its name and type.
 * 
 * @author Matthias Pohl
 * 
 * @see Schema
 */
public class ColumnInfo implements AutoJsonable {
	
	private final String name;
	private final int sqlType;

	/**
	 * Creates a <code>ColumnInfo</code> object with the name and the {@link java.sql.Types} data type representation of the column.
	 * 
	 * @param name
	 *            The name of the column.
	 * @param sqlType
	 *            A type value representing the data type of the column.
	 */
	public ColumnInfo(String name, int sqlType) {
		this.name = name;
		this.sqlType = sqlType;
	}

	/**
	 * Returns the column's name.
	 * 
	 * @return The column's name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the {@link java.sql.Types} datatype of the column.
	 * 
	 * @return The column's SQL datatype representation.
	 */
	public int getSQLType() {
		return this.sqlType;
	}

	@Override
	public int hashCode() {
		return 31 + ((this.name == null) ? 0 : this.name.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		
		ColumnInfo other = (ColumnInfo) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		
		return true;
	}
}
