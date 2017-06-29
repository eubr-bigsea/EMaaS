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
import java.io.Serializable;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

/**
 * <code>JsonNumber</code> represents a Json-convertible number.
 * 
 * @author Matthias Pohl
 */
public class JsonNumber implements JsonAtomic, Serializable {

	private static final Logger logger = Logger.getLogger(JsonNumber.class.getPackage().getName());

	private Number value;
	
	/**
	 * Initializes a <code>JsonNumber</code> with <code>0</code>.
	 */
	public JsonNumber() {
		this.value = 0;
	}

	/**
	 * Initializes a <code>JsonNumber</code>.
	 * 
	 * @param val
	 *            The actual value.
	 */
	public JsonNumber(Number val) {
		this.value = val;
	}

	/**
	 * Creates a <code>JsonNumber</code> instance based on the passed Json code.
	 * 
	 * @param jsonCode
	 *            The code that shall be converted into a <code>JsonNumber</code>
	 * @return The newly created <code>JsonNumber</code>.
	 * @throws ParseException
	 *             If the passed Json code cannot be converted into a <code>JsonNumber</code>.
	 */
	public static JsonNumber createJsonNumber(String jsonCode) throws ParseException {
		final JsonNumber number = new JsonNumber();
		try {
			number.fromJson(new GenericJsonParser<JsonValue>(JsonValue.class, jsonCode));
		} catch (IOException e) {
			JsonNumber.logger.error("IOException occurred within createJsonNumber(String) when processing \""
					+ jsonCode + "\".");
		}
		
		return number;
	}

	/**
	 * Returns <code>JsonType.Number</code>.
	 * 
	 * @see JsonValue#getType()
	 */
	public JsonType getType() {
		return JsonValue.JsonType.Number;
	}

	/**
	 * Returns the actual value.
	 * 
	 * @return The actual value.
	 */
	public Number getValue() {
		return this.value;
	}

	/**
	 * Returns <code>1</code> since <code>JsonNumber</code> is an atomic value.
	 * 
	 * @see JsonValue#size()
	 */
	public int size() {
		return 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonNumber other = (JsonNumber) obj;
		if (this.value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!this.value.equals(other.value)) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(JsonValue other) {
		if (other == null) {
			return 1;
		} else if (this == other) {
			return 0;
		} else if (this.getType().compareTo(other.getType()) != 0) {
			return this.getType().compareTo(other.getType());
		}
		
		JsonNumber otherNumber = (JsonNumber) other;

		if (this.value.getClass().equals(otherNumber.value.getClass())) {
			 // both numbers are instances of the same type!
			if (this.value instanceof Comparable<?>) {
				// and they implement the Comparable interface
				return ((Comparable<Number>) this.value).compareTo(otherNumber.value);
			}
		}
		
		// if they have different types, try to transform the numbers into double values
		// might result in unexpected behavior since double can represent every number
		if (this.value.doubleValue() < ((JsonNumber) other).value.doubleValue()) {
			return -1;
		} else if (this.value.doubleValue() > ((JsonNumber) other).value.doubleValue()) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeJsonNumber(this);
	}
	
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		this.value = jsonParser.nextJsonNumber().value;
	}

	public String getStringValue() {
		return this.value.toString();
	}

}
