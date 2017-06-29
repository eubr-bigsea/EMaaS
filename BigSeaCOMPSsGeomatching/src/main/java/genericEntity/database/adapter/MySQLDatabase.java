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
import java.util.Properties;

import genericEntity.database.util.DBInfo;

/**
 * <code>MySQLDatabase</code> encapsulates all the necessary information for establishing a connection to a MySQL database.
 * 
 * @author Matthias Pohl
 */
public class MySQLDatabase extends Database {

	/**
	 * Initializes the <code>MySQLDatabase</code> instance members and loads the settings provided by the parameter <code>dbInfo</code>.
	 * 
	 * @param dbInfo
	 *            Information needed for establishing a connection to this database.
	 */
	public MySQLDatabase(DBInfo dbInfo) {
		super(dbInfo);
	}

	/**
	 * Initializes the <code>MySQLDatabase</code> using the passed {@link InputStream}. The information provided by this stream has to convertible
	 * into a {@link Properties} instance.
	 * 
	 * @param iStream
	 *            The <code>InputStream</code> that provides the connection information.
	 */
	public MySQLDatabase(InputStream iStream) {
		super(iStream);
	}

	/**
	 * Initializes the <code>MySQLDatabase</code> using the passed {@link Properties}.
	 * 
	 * @param prop
	 *            The <code>Properties</code> instance that provides the connection information.
	 */
	public MySQLDatabase(Properties prop) {
		super(prop);
	}
	
	@Override
	public String getDatabaseDriverName() {		
		return "org.gjt.mm.mysql.Driver";
	}

	@Override
	public String getJDBCString() {
		return "jdbc:mysql://" + this.getHost() + ":" + this.getPort() + "/" + this.getDatabaseName();
	}

}
