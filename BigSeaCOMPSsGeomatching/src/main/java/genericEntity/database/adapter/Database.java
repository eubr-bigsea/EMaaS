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

package genericEntity.database.adapter;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import genericEntity.database.util.DBInfo;
import genericEntity.util.data.AutoJsonable;

/**
 * <code>Database</code> is an abstract class that encapsulates the database related information. It is used to simply add new databases to the tool.
 * A new database class has to extend <code>Database</code>. Additionally, the JDBC library for that database has to be added.
 * 
 * @author Matthias Pohl
 */
public abstract class Database implements AutoJsonable {

	/**
	 * The information needed for establishing a connection to the database.
	 */
	private final DBInfo databaseInfo;

	/**
	 * Initializes the database instance members and loads the settings provided by the parameter <code>dbInfo</code>.
	 * 
	 * @param dbInfo
	 *            Information needed for establishing a connection to this database.
	 */
	public Database(DBInfo dbInfo) {
		if (dbInfo == null) {
			throw new NullPointerException("null was passed.");
		}

		this.databaseInfo = dbInfo;
	}

	/**
	 * Initializes the database using the passed {@link InputStream}. The information provided by this stream has to convertible into a
	 * {@link Properties} instance.
	 * 
	 * @param iStream
	 *            The <code>InputStream</code> that provides the connection information.
	 */
	public Database(InputStream iStream) {
		this(new DBInfo());
		this.loadDatabaseInformation(iStream);
	}

	/**
	 * Initializes the database using the passed {@link Properties}.
	 * 
	 * @param prop
	 *            The <code>Properties</code> instance that provides the connection information.
	 */
	public Database(Properties prop) {
		this(new DBInfo());
		this.loadDatabaseInformation(prop);
	}

	/**
	 * Loads the data from the {@link InputStream}.
	 * 
	 * @param iStream
	 *            The stream from which the database information are read.
	 */
	public void loadDatabaseInformation(InputStream iStream) {
		if (iStream == null) {
			throw new NullPointerException("No InputStream was passed.");
		}

		this.databaseInfo.loadProperties(iStream);
	}

	/**
	 * Loads the data from a {@link Properties} instance.
	 * 
	 * @param prop
	 *            The properties from which the database information are read.
	 */
	public void loadDatabaseInformation(Properties prop) {
		if (prop == null) {
			throw new NullPointerException("No Properties instance was passed.");
		}

		this.databaseInfo.loadProperties(prop);
	}

	/**
	 * Returns the schema, which is used by this database connection.
	 * 
	 * @return SQL Schema used by this connection.
	 */
	public String getSQLSchema() {
		return this.databaseInfo.getSQLSchema();
	}

	/**
	 * Returns the host information of the underlying database system.
	 * 
	 * @return The host information of the underlying database system.
	 */
	public String getHost() {
		return this.databaseInfo.getHost();
	}

	/**
	 * Returns the port of the underlying database system.
	 * 
	 * @return The port of the underlying database system.
	 */
	public int getPort() {
		return this.databaseInfo.getPort();
	}

	/**
	 * Returns the name of the database.
	 * 
	 * @return The name of the database.
	 */
	public String getDatabaseName() {
		return this.databaseInfo.getDatabaseName();
	}

	/**
	 * Returns a {@link Connection} object, which represents a new connection to the database.
	 * 
	 * @return <code>Connection</code> object used for this database.
	 * @throws SQLException
	 *             If the connection could not be established.
	 * @throws ClassNotFoundException If the Driver class could not be loaded.
	 */
	public Connection createConnection() throws SQLException, ClassNotFoundException {
		Class.forName(this.getDatabaseDriverName());
		return DriverManager.getConnection(getJDBCString(), this.databaseInfo.getUser(), this.databaseInfo.getPassword());
	}

	/**
	 * Checks whether two <code>Database</code> instances have the same information stored.
	 * 
	 * @param other
	 *            The other <code>Database</code> instance.
	 * @return <code>true</code>, if all the information is the same; otherwise <code>false</code>.
	 */
	public boolean equals(Database other) {
		return this.databaseInfo.equals(other.databaseInfo);
	}

	/**
	 * Returns the JDBC String which can be used for representing a connection to this database.
	 * 
	 * @return The JDBC String for this database connection.
	 */
	public abstract String getJDBCString();

	/**
	 * Returns the <code>Driver</code>'s name used for loading the Driver class. The corresponding library has to be added to the build path.
	 * 
	 * @return The <code>Driver</code> class name which is used for the database connection.
	 */
	public abstract String getDatabaseDriverName();
}
