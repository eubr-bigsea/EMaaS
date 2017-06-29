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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;

import genericEntity.util.data.Jsonable;

/**
 * A <code>JsonableWriter</code> can be used to add data to a {@link JsonWritable}.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The type of data that can be added.
 */
public interface JsonableWriter<T extends Jsonable> extends Closeable {

	/**
	 * Adds the passed value to the underlying storage.
	 * 
	 * @param value
	 *            The value that shall be added.
	 * @return <code>true</code>, if it was added; otherwise <code>false</code>.
	 * @throws JsonGenerationException
	 *             If an error occurred while converting the passed object into Json.
	 * @throws IOException
	 *             If an error occurred while sending the data to the stream.
	 */
	public boolean add(T value) throws JsonGenerationException, IOException;

	/**
	 * Adds content of the passed collection to the underlying {@link GenericObjectStorage}.
	 * 
	 * @param collection
	 *            The collection whose content shall be added.
	 * @return <code>true</code>, if the content of the storage changed by reason of calling this method; otherwise <code>false</code>.
	 * @throws JsonGenerationException
	 *             If an error occurred while converting the passed object into Json.
	 * @throws IOException
	 *             If an error occurred while sending the data to the stream.
	 */
	public boolean addAll(Collection<T> collection) throws JsonGenerationException, IOException;
}
