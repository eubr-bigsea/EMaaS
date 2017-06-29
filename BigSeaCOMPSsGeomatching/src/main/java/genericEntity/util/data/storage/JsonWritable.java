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

package genericEntity.util.data.storage;

import java.io.IOException;

import genericEntity.util.data.Jsonable;

/**
 * <code>JsonWritable</code> is an interface for adding writable functionality to some {@link Jsonable} storage.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The type of instances that shall be added.
 */
public interface JsonWritable<T extends Jsonable> {

	/**
	 * Returns the {@link JsonableWriter} that can be used to add instances to this <code>GenericObjectStorage</code>.
	 * 
	 * @return The <code>JsonableWriter</code> for adding content to this storage.
	 * @throws IOException
	 *             If an error occurs while writing data.
	 */
	public JsonableWriter<T> getWriter() throws IOException;

	/**
	 * Checks whether formatted Json is enabled in this storage.
	 * 
	 * @return <code>true</code>, if it is enabled; otherwise <code>false</code>.
	 */
	public boolean isFormattedJson();

	/**
	 * Enables formatted Json for this storage.
	 */
	public void enableFormattedJson();

	/**
	 * Disables formatted Json for this storage.
	 */
	public void disableFormattedJson();
}
