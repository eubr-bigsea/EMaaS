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

import genericEntity.util.data.Jsonable;

/**
 * <code>JsonReadable</code> is an interface for adding readable functionality to some {@link Jsonable} storage.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The type of instances that shall be added.
 */
public interface JsonReadable<T extends Jsonable> {

	/**
	 * Returns the {@link JsonableReader} that can be used to access the content of this <code>GenericObjectStorage</code>.
	 * 
	 * @return The <code>JsonableReader</code> for iterating over the content.
	 */
	public JsonableReader<T> getReader();

	/**
	 * Returns the number of instances that are contained.
	 * 
	 * @return The size of this storage.
	 */
	public int size();

}
