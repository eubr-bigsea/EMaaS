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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import genericEntity.util.data.AutoJsonable;

/**
 * <code>DBInfo</code> encapsulates the settings which are needed for establishing a database connection.
 * 
 * @author Matthias Pohl
 */
public class DBInfo implements AutoJsonable {

	private static final Logger logger = Logger.getLogger(DBInfo.class.getPackage().getName());

	/**
	 * Parameter name of the database host.
	 */
	public static final String STR_DATABASE_HOST = "DATABASE_HOST";

	/**
	 * Parameter name of the database port.
	 */
	public static final String STR_DATABASE_PORT = "DATABASE_PORT";

	/**
	 * Parameter name of the database name.
	 */
	public static final String STR_DATABASE_NAME = "DATABASE_NAME";

	/**
	 * Parameter name of the database user.
	 */
	public static final String STR_DATABASE_USER = "DATABASE_USER";

	/**
	 * Parameter name of the database password.
	 */
	public static final String STR_DATABASE_PASSWORD = "DATABASE_PASSWORD";

	/**
	 * Parameter name of the database schema.
	 */
	public static final String STR_DATABASE_SCHEMA = "DATABASE_SCHEMA";

	private String host;
	private int port;
	private String databaseName;
	private String user;
	private String password;
	private String sqlSchema;

	/**
	 * Initializes a <code>DBInfo</code> instance with no initial information.
	 */
	public DBInfo() {
		// nothing to do
	}

	/**
	 * Initializes a <code>DBInfo</code> instance with the passed properties.
	 * 
	 * @param properties
	 *            The initial properties.
	 */
	public DBInfo(Properties properties) {
		this.loadProperties(properties);
	}

	/**
	 * Initializes a <code>DBInfo</code> instance where the initial properties are read from the passed {@link InputStream}.
	 * 
	 * @param stream
	 *            The stream from which the properties will be read.
	 */
	public DBInfo(InputStream stream) {
		this.loadProperties(stream);
	}

	/**
	 * Initializes a <code>DBInfo</code> instance with the properties provided by the properties file whose path was passed.
	 * 
	 * @param propertiesPath
	 *            The path to the properties file.
	 * @throws FileNotFoundException
	 *             If no file was found.
	 */
	public DBInfo(String propertiesPath) throws FileNotFoundException {
		this.loadProperties(propertiesPath);
	}

	/**
	 * Loads the settings into this <code>DBInfo</code> object using a <code>Properties</code> table.
	 * 
	 * @param dbInfo
	 *            Information needed for establishing a database connection
	 */
	public void loadProperties(Properties dbInfo) {
		this.setHost(dbInfo.getProperty(STR_DATABASE_HOST, ""));
		this.setPort(dbInfo.getProperty(STR_DATABASE_PORT, "-1"));
		this.setDatabaseName(dbInfo.getProperty(STR_DATABASE_NAME, ""));
		this.setUser(dbInfo.getProperty(STR_DATABASE_USER, ""));
		this.setPassword(dbInfo.getProperty(STR_DATABASE_PASSWORD, ""));
		this.setSQLSchema(dbInfo.getProperty(STR_DATABASE_SCHEMA, ""));
	}

	/**
	 * 
	 * 
	 * @param jsonObject
	 *            Information needed for establishing a database connection
	 */
	public void loadProperties(JSONObject jsonObject) {
		this.setHost((String)jsonObject.get(STR_DATABASE_HOST));
		this.setPort((String)jsonObject.get(STR_DATABASE_PORT));
		this.setDatabaseName((String)jsonObject.get(STR_DATABASE_NAME));
		this.setUser((String)jsonObject.get(STR_DATABASE_USER));
		this.setPassword((String)jsonObject.get(STR_DATABASE_PASSWORD));
		this.setSQLSchema((String)jsonObject.get(STR_DATABASE_SCHEMA));
	}
	
	/**
	 * Loads the settings out of a stream into the <code>DBInfo</code> object.
	 * 
	 * @param input
	 *            The stream from which the data shall be read.
	 */
	public void loadProperties(InputStream input) {
		Properties data = new Properties();
		try {
			data.load(input);
		} catch (IOException e) {
			DBInfo.logger.warn("Unable to load data out of the passed stream.", e);
		}

		this.loadProperties(data);
	}

	/**
	 * Loads the properties out of a file specified by its path.
	 * 
	 * @param propFilePath
	 *            The path to the properties file.
	 * @throws FileNotFoundException
	 *             If the file specified by the passed path does not exist.
	 */
	public void loadProperties(String propFilePath) throws FileNotFoundException {
		JSONParser parser = new JSONParser();
        try {
 
            Object obj = parser.parse(new FileReader(propFilePath));
            JSONObject jsonObject = (JSONObject) obj;
            this.loadProperties(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
		
	}

	/**
	 * Checks whether all necessary properties are set (host, port, database name, user and password).
	 * 
	 * @return <code>true</code>, if the properties, mentioned above, are set; otherwise <code>false</code>
	 */
	public boolean allNecessaryPropertiesAreSet() {
		boolean allNecessaryPropAreSet = true;

		if (this.host.equals("")) {
			DBInfo.logger.warn("DBInfo: The host isn't set.");
			allNecessaryPropAreSet = false;
		}

		if (this.port == -1) {
			DBInfo.logger.warn("DBInfo: The port isn't set.");
			allNecessaryPropAreSet = false;
		}

		if (this.databaseName.equals("")) {
			DBInfo.logger.warn("DBInfo: The name of the database isn't set.");
			allNecessaryPropAreSet = false;
		}

		if (this.user.equals("")) {
			DBInfo.logger.warn("DBInfo: The user isn't set.");
			allNecessaryPropAreSet = false;
		}

		if (this.password.equals("")) {
			DBInfo.logger.warn("DBInfo: The password isn't set.");
			allNecessaryPropAreSet = false;
		}

		return allNecessaryPropAreSet;
	}

	/**
	 * Returns the host of the currently used database system.
	 * 
	 * @return The host.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host of the currently used database system.
	 * 
	 * @param host
	 *            The new host.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns the port of the currently used database system.
	 * 
	 * @return the port The port.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the currently used database system.
	 * 
	 * @param port
	 *            The new port.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Sets the port of the currently used database system.
	 * 
	 * @param port
	 *            The new port.
	 */
	public void setPort(String port) {
		this.port = Integer.parseInt(port);
	}

	/**
	 * Returns the name of the currently used database.
	 * 
	 * @return The database name.
	 */
	public String getDatabaseName() {
		return this.databaseName;
	}

	/**
	 * Sets a new database.
	 * 
	 * @param databaseName
	 *            The new database.
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	/**
	 * Returns the user name which is used for establishing the current database connection.
	 * 
	 * @return The user name.
	 */
	public String getUser() {
		return this.user;
	}

	/**
	 * Sets the user name which is used for establishing the current database connection.
	 * 
	 * @param user
	 *            The new user name.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the password which is used for establishing the current database connection.
	 * 
	 * @return The password.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password which is used for establishing the current database connection.
	 * 
	 * @param password
	 *            The new password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the sqlSchema which is used in the current database connection.
	 * 
	 * @return The sqlSchema or an empty String if no sqlSchema was set.
	 */
	public String getSQLSchema() {
		if (this.sqlSchema == null) {
			return "";
		}

		return this.sqlSchema;
	}

	/**
	 * Set a new sqlSchema.
	 * 
	 * @param schema
	 *            The new sqlSchema.s
	 */
	public void setSQLSchema(String schema) {
		this.sqlSchema = schema;
	}

	/**
	 * Checks whether two <code>DBInfo</code> instances have the same information stored.
	 * 
	 * @param other
	 *            The other <code>DBInfo</code> instance.
	 * @return <code>true</code>, if all the information is the same; otherwise <code>false</code>.
	 */
	public boolean equals(DBInfo other) {
		return other != null && this.databaseName.equals(other.databaseName) && this.host.equals(other.host) && this.password.equals(other.password)
				&& this.sqlSchema.equals(other.sqlSchema) && this.user.equals(other.user) && this.port == other.port;
	}
}
