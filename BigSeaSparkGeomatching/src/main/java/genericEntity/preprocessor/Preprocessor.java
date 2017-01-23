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

package genericEntity.preprocessor;

import genericEntity.datasource.DataSource;
import genericEntity.util.data.GenericObject;

/**
 * <code>Preprocessor</code> is an <code>interface</code> that can be used for gathering statistics of the data
 * within the extraction phase.
 * 
 * @author Matthias Pohl
 */
public interface Preprocessor {
	//TODO: implement method for manipulating (i.e., normalizing) data (if it is done here, it is done only once instead of doing it over and over again in a similarity function)

	/**
	 * Passes the currently extracted {@link GenericObject} to the <code>Preprocessor</code> for further analysis. This
	 * method is called by every {@link DataSource} per extracted data record.
	 * 
	 * @param data
	 *            The <code>GenericObject</code> that shall be analyzed.
	 */
	public void analyzeGenericObject(final GenericObject data);

	/**
	 * This method is called after finishing the data extraction process. It can be used in order to created some
	 * further statistics.
	 */
	public void finish();

	/**
	 * Clears statistics that were already gathered.
	 */
	public void clearData();

}
