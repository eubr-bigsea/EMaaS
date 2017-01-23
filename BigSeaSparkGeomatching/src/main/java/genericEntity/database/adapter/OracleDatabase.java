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
 * <code>OracleDatabase</code> encapsulates all the necessary information for establishing a connection to an Oracle database.
 * <p>
 * CAUTION! This <code>Database</code> implementation can be used with Oracle's Thin drivers only (if you are using OCI drivers, you will have to
 * implement your own {@link Database} adapter class). <code>Generic</code> cannot offer this library due to licensing issues. In order to connect
 * <code>Generic</code> with an Oracle database, you have to download the library and add it to your classpath manually. The driver can be found on the
 * following homepage: <url>http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html</url>
 * 
 * <p>
 * Be aware of that this class does only support the JDBC Thin driver!
 * 
 * @author Uwe Draisbach
 */
public class OracleDatabase extends Database {

	/**
	 * Initializes the <code>OracleDatabase</code> instance members and loads the settings provided by the parameter <code>dbInfo</code>.
	 * 
	 * @param dbInfo
	 *            Information needed for establishing a connection to this database.
	 */
	public OracleDatabase(DBInfo dbInfo) {
		super(dbInfo);
	}

	/**
	 * Initializes the <code>OracleDatabase</code> using the passed {@link InputStream}. The information provided by this stream has to convertible
	 * into a {@link Properties} instance.
	 * 
	 * @param iStream
	 *            The <code>InputStream</code> that provides the connection information.
	 */
	public OracleDatabase(InputStream iStream) {
		super(iStream);
	}

	/**
	 * Initializes the <code>OracleDatabase</code> using the passed {@link Properties}.
	 * 
	 * @param prop
	 *            The <code>Properties</code> instance that provides the connection information.
	 */
	public OracleDatabase(Properties prop) {
		super(prop);
	}

	@Override
	public String getDatabaseDriverName() {
		return "oracle.jdbc.OracleDriver";
	}

	@Override
	public String getJDBCString() {
		return "jdbc:oracle:thin:@" + this.getHost() + ":" + this.getPort() + "/" + this.getDatabaseName();
	}

}
