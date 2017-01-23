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

/**
 * 
 */
package genericEntity.util.data.json.auto;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;

/**
 * Json serialization for basic types including primitive types and boxing classes as well as Strings.
 * 
 * @author Arvid.Heise
 * 
 * @param <T>
 *            the primitive type or boxing class
 */
public abstract class Primitive<T> {
	private final Class<T> type;

	/**
	 * Initializes the primitive for given type
	 * 
	 * @param type
	 */
	protected Primitive(Class<T> type) {
		this.type = type;
	}

	/**
	 * Returns the type of the primitive
	 * 
	 * @return type of the primitive
	 */
	public Class<T> getType() {
		return this.type;
	}

	/**
	 * Reads the next value from the parser and returns it.
	 * 
	 * @param parser
	 *            the parser
	 * @return an instance of {@link #getType()}
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	abstract T read(GenericJsonParser<?> parser) throws JsonParseException, IOException;

	/**
	 * Writes the given primitive to the json stream.
	 * 
	 * @param object
	 *            the object to write
	 * @param generator
	 *            the {@link GenericJsonGenerator} rendering the passed value to json
	 * 
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	abstract void write(T object, GenericJsonGenerator generator) throws JsonGenerationException, IOException;

}