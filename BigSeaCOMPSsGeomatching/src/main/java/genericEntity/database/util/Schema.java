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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import genericEntity.exception.InvalidSchemaException;
import genericEntity.util.data.AutoJsonable;

/**
 * The <code>Schema</code> encapsulates all the information concerning a database table schema.
 * 
 * @author Matthias Pohl
 */
public class Schema implements Iterable<ColumnInfo>, AutoJsonable {

	/**
	 * A list of all column representations which are part of this schema.
	 */
	protected final List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

	/**
	 * Initializes a <code>Schema</code> using a given collection of {@link ColumnInfo} instances.
	 * 
	 * @param cols
	 *            A collection of <code>ColumnInfo</code> instances representing all columns of this <code>Schema</code>.
	 * @throws InvalidSchemaException
	 *             If the column names are not unique.
	 */
	public Schema(Collection<ColumnInfo> cols) throws InvalidSchemaException {
		this.columns.addAll(cols);

		if (!this.columnNamesAreUnique()) {
			throw new InvalidSchemaException("Some column names in the schema aren't unique:\n" + this.columns);
		}
	}

	/**
	 * Initializes the <code>Schema</code> out of the passed table.
	 * 
	 * @param conn
	 *            The connection to the table's database.
	 * @param sqlSchema
	 *            The SQL schema of the table.
	 * @param tableName
	 *            The name of the table.
	 * @throws InvalidSchemaException
	 *             If an error occurred while extracting the <code>Schema</code> information.
	 */
	public Schema(Connection conn, String sqlSchema, String tableName) throws InvalidSchemaException {
		try {
			DatabaseMetaData metaData = conn.getMetaData();
			ResultSet result = metaData.getColumns(null, sqlSchema, tableName, null);

			while (result.next()) {
				this.columns.add(new ColumnInfo(result.getString("COLUMN_NAME"), result.getInt("DATA_TYPE")));
			}

			result.close();
		} catch (SQLException e) {
			throw new InvalidSchemaException("Couldn't get metadata of table '" + sqlSchema + "." + tableName + "'.", e);
		}
	}

	/**
	 * Checks whether the set columns are unique.
	 * 
	 * @return <code>true</code>, if each column has a unique name; otherwise <code>false</code>.
	 */
	protected boolean columnNamesAreUnique() {
		return new HashSet<ColumnInfo>(this.columns).size() != this.columns.size();
	}

	/**
	 * Returns the schema's column count.
	 * 
	 * @return The number of columns.
	 */
	public int size() {
		return this.columns.size();
	}

	/**
	 * Returns the information of the column with the given index.
	 * 
	 * @param index
	 *            The position of the column in the schema.
	 * @return A {@link ColumnInfo} of the column located at position <code>index</code> in the schema.
	 */
	protected ColumnInfo getColumnInfo(int index) {
		return this.columns.get(index);
	}
	
	/**
	 * Returns the information of the column with the given name.
	 * 
	 * @param name
	 *            The name of the column in the schema.
	 * @return A {@link ColumnInfo} of the column located at position <code>index</code> in the schema.
	 */
	protected ColumnInfo getColumnInfo(String name) {
		if (name.contains("(")) {
			int beginIndex = name.lastIndexOf("(") + 1;
			int endIndex = name.lastIndexOf(")");
			name = name.substring(beginIndex, endIndex);
		}
		if (name.toLowerCase().contains(" as ")) {			
			int indexAlias = name.toLowerCase().lastIndexOf(" as ");
			// +4 because of the " as "
			name = name.substring(0, indexAlias);
			
		}
		for (ColumnInfo column : this.columns) {
			if (column.getName().equals(name)) {
				return column;
			}
		}
		return null;
	}

	/**
	 * Returns the data type of the specified column.
	 * 
	 * @param columnIndex
	 *            The position of the column.
	 * @return The data type java.sql.Types representation of the column's data type.
	 * 
	 * @see Types
	 */
	public int getColumnSQLType(int columnIndex) {
		return this.getColumnInfo(columnIndex).getSQLType();
	}
	
	/**
	 * Returns the data type of the specified column.
	 * 
	 * @param columnName
	 *            The name of the column.
	 * @return The data type java.sql.Types representation of the column's data type.
	 * @throws Exception 
	 * 			If the given name was not found in the schema.
	 * @see Types
	 */
	public int getColumnSQLType(String columnName) throws Exception {		
		ColumnInfo info = this.getColumnInfo(columnName);
		if (info == null) {
			throw new Exception("Name of column not found.");
		}
		return this.getColumnInfo(columnName).getSQLType();
	}

	/**
	 * Returns the name of the specified column.
	 * 
	 * @param columnIndex
	 *            The position of the column.
	 * @return The name of the column.
	 */
	public String getColumnName(int columnIndex) {
		return this.getColumnInfo(columnIndex).getName();
	}

	/**
	 * Returns an iterator that iterates over the <code>Schema</code> instance.
	 * 
	 * @see Iterable#iterator()
	 */
	public Iterator<ColumnInfo> iterator() {
		return this.columns.iterator();
	}

	/**
	 * Checks whether the current schema contains no columns.
	 * 
	 * @return <code>true</code>, if no columns are part of this schema; otherwise <code>false</code>.
	 */
	public boolean isEmpty() {
		return this.size() == 0;
	}
}
