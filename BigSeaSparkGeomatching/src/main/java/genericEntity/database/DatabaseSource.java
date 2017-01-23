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

package genericEntity.database;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import genericEntity.database.adapter.Database;
import genericEntity.database.util.Schema;
import genericEntity.datasource.AbstractDataSource;
import genericEntity.datasource.DataSource;
import genericEntity.exception.ExtractionFailedException;
import genericEntity.exception.InvalidSchemaException;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.JsonNull;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.json.JsonString;
import genericEntity.util.data.json.JsonValue;

/**
 * <code>DatabaseSource</code> represents databases.
 * 
 * @author Matthias Pohl
 * 
 * @see Database
 * @see DBConnector
 */
public class DatabaseSource extends AbstractDataSource<DatabaseSource> {

	/**
	 * <code>DatabaseSourceIterator</code> is used for generating
	 * {@link GenericObject}s out of <code>DatabaseSource</code>s.
	 * 
	 * @author Matthias Pohl
	 */
	protected class DatabaseSourceIterator extends AbstractDataSourceIterator<DatabaseSource> {

		private Connection connection;
		private Schema tableSchema;
		private ResultSet result;

		/**
		 * Initializes a <code>DatabaseSourceIterator</code> using the passed
		 * <code>DatabaseSource</code>.
		 * 
		 * @param source
		 *            The source of which the data shall be extracted.
		 * @throws SQLException
		 *             If an error occurred while requesting the data.
		 */
		protected DatabaseSourceIterator(DatabaseSource source) throws SQLException {
			super(source);

			try {
				this.connection = this.dataSource.getDatabase().createConnection();
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("The database driver could not be loaded.", e);
			}
			Statement stmt = this.connection.createStatement();

			try {
				this.tableSchema = new Schema(this.connection, this.dataSource.getDatabase().getSQLSchema(),
						this.dataSource.getTableName());
			} catch (InvalidSchemaException e) {
				throw new IllegalStateException("The schema could not be extracted.", e);
			}

			this.result = stmt.executeQuery(this.dataSource.getQuery());
			this.dataSource.registerStatement(stmt);
		}

		private void closeResultSet() {
			if (!this.resultSetClosed()) {
				try {
					this.result.close();
				} catch (final SQLException e) {
					DatabaseSource.logger.warn("Error occurred while closing the underlying resultSet.", e);
				} finally {
					this.result = null;
				}
			}
		}

		private boolean resultSetClosed() {
			return this.result == null;
		}

		@Override
		protected JsonRecord loadNextRecord() throws ExtractionFailedException {
			if (this.resultSetClosed()) {
				return null;
			}

			JsonRecord jsonRecord = null;

			try {
				if (this.result.next()) {
					jsonRecord = new JsonRecord();
					ResultSetMetaData metadata = result.getMetaData();

					for (int columnIndex = 1; columnIndex <= metadata.getColumnCount(); ++columnIndex) {
						JsonValue value = new JsonString(this.result.getString(columnIndex));										

						if (this.result.wasNull()) {
							value = JsonNull.NULL;
						}

						if (columnIndex < this.tableSchema.size()) {
							this.addAttributeValue(jsonRecord, metadata.getColumnLabel(columnIndex), value);
						}
					}

				} else {
					this.closeResultSet();
				}
				
			} catch (final SQLException e) {
				DatabaseSource.logger
						.fatal("An SQLException was raised while extracting the data with the following connection:\n\t"
								+ this.dataSource.getJDBCString() + "\nQuery:\n" + this.dataSource.getQuery(), e);
				throw new ExtractionFailedException("An SQLException occurred - connection: "
						+ this.dataSource.getJDBCString() + "; Query: " + this.dataSource.getQuery(), e);
			}

			return jsonRecord;
		}
	}

	private static final Logger logger = Logger.getLogger(DatabaseSource.class.getPackage().getName());
	public static final String STR_IDENTIFIER = "IDENTIFIER";
	public static final String STR_IS_OMS = "IS_OMS";
	public static final String STR_QUERY = "QUERY";

	private transient final Collection<Statement> openedStatements = new ArrayList<Statement>();
	
	private Database database;
	private String tableName;
	private boolean isOMS;
	private String identifier;
	private String query;

	/**
	 * Internal constructor for {@link Jsonable} deserialization.
	 */
	protected DatabaseSource() {
		// nothing to do
	}

	/**
	 * Initializes <code>DatabaseSource</code> for the passed {@link Database}
	 * and table.
	 * 
	 * @param identifier
	 *            The identifier of this {@link DataSource}.
	 * @param db
	 *            The underlying <code>Database</code>.
	 * @param tbName
	 *            The table name.
	 */
	public DatabaseSource(String identifier, Database db, String tbName, boolean isOMS) {
		super(identifier);

		if (db == null) {
			throw new NullPointerException("DB connection is missing.");
		} else if (tbName == null) {
			throw new NullPointerException("Table name is missing.");
		}

		this.isOMS = isOMS;
		this.database = db;
		this.tableName = tbName;
	}

	public DatabaseSource(String pathJsonFile, Database db) {
		super();

		if (db == null) {
			throw new NullPointerException("DB connection is missing.");
		}

		this.database = db;

		JSONParser parser = new JSONParser();
		try {

			Object obj = parser.parse(new FileReader(pathJsonFile));
			JSONObject jsonObject = (JSONObject) obj;

			this.identifier = (String) jsonObject.get(STR_IDENTIFIER);
			this.isOMS = (boolean) jsonObject.get(STR_IS_OMS);
			this.query = (String) jsonObject.get(STR_QUERY);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public boolean isOMS() {
		return this.isOMS;
	}

	public void setIsOMS(boolean oms) {
		this.isOMS = oms;
	}

	/**
	 * Returns the underlying {@link Database}.
	 * 
	 * @return The underlying <code>Database</code>.
	 */
	protected Database getDatabase() {
		return this.database;
	}

	/**
	 * Returns the table name.
	 * 
	 * @return The table name.
	 */
	protected String getTableName() {
		return this.tableName;
	}

	protected void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Returns the JDBC String of the underlying database.
	 * 
	 * @return The JDBC String of the underlying database.
	 */
	protected String getJDBCString() {
		return this.database.getJDBCString();
	}

	/**
	 * Registers a {@link Statement}. This statement will be closed during the
	 * next call of {@link #cleanUp()}.
	 * 
	 * @param stmt
	 *            The statement that shall be closed later on.
	 */
	protected void registerStatement(Statement stmt) {
		if (stmt == null) {
			return;
		}

		this.openedStatements.add(stmt);
	}

	@Override
	public void cleanUp() {
		super.cleanUp();

		for (Statement stmt : this.openedStatements) {
			try {
				stmt.close();
			} catch (SQLException e) {
				DatabaseSource.logger.warn("An SQLException occurred while closing an opened statement.", e);
			}
		}

		this.openedStatements.clear();
	}

	@Override
	public Iterator<GenericObject> iterator() {
		try {
			return new DatabaseSourceIterator(this);
		} catch (SQLException e) {
			throw new IllegalStateException("A SQLException occurred while instantiating the DatabaseSource iterator.",
					e);
		}
	}
}