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

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

/**
 * <code>JsonNull</code> represents the Json <code>null</code> value. Parts of the code are reused and
 * were taken from the JSON.org implementation.
 * 
 * @author Matthias Pohl
 */
public class JsonNull implements JsonAtomic {
	
	/**
	 * The Json <code>null</code> value.
	 */
	public static final JsonNull NULL = new JsonNull();
	
	private JsonNull() {
		// do nothing
	}

	/**
	 * There is only intended to be a single instance of the NULL object, so the clone method returns itself.
	 * 
	 * @return NULL.
	 */
	@Override
	protected final Object clone() {
		return this;
	}

	/**
	 * A <code>JsonRecord.JsonNull</code> object is equal to the null value and to itself.
	 * 
	 * @param object
	 *            An object to test for being <code>null</code>.
	 * @return <code>true</code> if the object parameter is the <code>JsonRecord.JsonNull</code> object or
	 *         <code>null</code>; otherwise <code>false</code>.
	 */
	@Override
	public boolean equals(Object object) {
		return object == null || object == this;
	}

	/**
	 * Returns the "null" String value.
	 * 
	 * @return The String "null".
	 */
	@Override
	public String toString() {
		return "null";
	}

	/**
	 * Returns <code>JsonType.Null</code>.
	 * 
	 * @see JsonValue#getType()
	 */
	public JsonType getType() {
		return JsonType.Null;
	}

	/**
	 * Returns <code>0</code> since <code>JsonRecord.JsonNull</code> does not have any value.
	 * 
	 * @see JsonValue#size()
	 */
	public int size() {
		return 0;
	}

	public int compareTo(JsonValue other) {
		if (other == null || this == other || other.getClass().equals(JsonNull.class)) {
			return 0;
		}

		return -1;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeJsonNull();
	}
	
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		// expects Json null value
		jsonParser.nextJsonNull();
	}

	public String getStringValue() {
		return "null";
	}
}
