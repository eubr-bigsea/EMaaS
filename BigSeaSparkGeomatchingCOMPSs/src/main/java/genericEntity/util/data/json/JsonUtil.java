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

package genericEntity.util.data.json;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

import genericEntity.util.data.AutoJsonable;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.auto.AutoJsonSerialization;
import genericEntity.util.data.json.auto.JsonTypeManager;

/**
 * Convenience methods for dealing with {@link Jsonable}s.
 * 
 * @author Arvid Heise
 */
public class JsonUtil {
	/**
	 * Serializes a {@link Jsonable} and returns the resulting json string.
	 * 
	 * @param jsonable
	 *            the {@link Jsonable} to serialize.
	 * @param formatted
	 *            if the output should be formatted
	 * @return the string representing the given {@link Jsonable}
	 */
	public static String toJson(AutoJsonable jsonable, boolean formatted) {
		StringWriter writer = new StringWriter();
		try {
			GenericJsonGenerator genericJsonGenerator = new GenericJsonGenerator(writer);
			if (formatted)
				genericJsonGenerator.enableFormattedJson();
			genericJsonGenerator.writeRecord(jsonable);
		} catch (JsonGenerationException e) {
			throw new IllegalArgumentException("could not properly write the jsonable", e);
		} catch (IOException e) {
			throw new IllegalStateException("string should not throw IOException", e);
		}
		return writer.toString();
	}

	/**
	 * Serializes a {@link Jsonable} and returns the resulting json string.
	 * 
	 * @param jsonable
	 *            the {@link Jsonable} to serialize.
	 * @return the string representing the given {@link Jsonable}
	 */
	public static String toJson(AutoJsonable jsonable) {
		return toJson(jsonable, false);
	}

	/**
	 * Parses the given json string to an instance of the given {@link Jsonable} type.
	 * 
	 * @param <T>
	 *            the specific {@link Jsonable} type
	 * @param json
	 *            the json string containing the representation of the new instance
	 * @param type
	 *            the specific {@link Jsonable} type
	 * @return a new instance of the type initialized with the given json string
	 */
	public static <T extends AutoJsonable> T fromJson(String json, Class<T> type) {
		GenericJsonParser<?> parser = null;
		try {
			parser = new GenericJsonParser<JsonValue>(json);
			return parser.nextObject(type);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("could not parse the json string", e);
		} catch (IOException e) {
			throw new IllegalStateException("string should not throw IOException", e);
		}
	}

	/**
	 * Reads all non-transient fields of the jsonable to the {@link GenericJsonParser}. It expects the given jsonParser to currently have an opened
	 * record and it leaves it open.
	 * 
	 * @param jsonParser
	 *            the {@link GenericJsonParser} to read from
	 * @param jsonable
	 *            the {@link AutoJsonable} to initialize
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading the data.
	 */
	@SuppressWarnings("unchecked")
	public static void readFields(GenericJsonParser<?> jsonParser, AutoJsonable jsonable) throws JsonParseException, IOException {
		((AutoJsonSerialization) JsonTypeManager.getInstance().getTypeInfo(jsonable.getClass())).readFields(jsonParser, jsonable);
	}

	/**
	 * Writes all non-transient fields of the jsonable to the {@link GenericJsonGenerator}. It expects the given jsonGenerator to currently have an
	 * opened record and it leaves it open.
	 * 
	 * @param jsonGenerator
	 *            the {@link GenericJsonGenerator} to write to
	 * @param jsonable
	 *            the {@link AutoJsonable} to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public static void writeFields(GenericJsonGenerator jsonGenerator, AutoJsonable jsonable) throws JsonGenerationException, IOException {
		JsonTypeManager.getInstance().getTypeInfo(jsonable.getClass()).writeFields(jsonGenerator, jsonable);
	}

}
