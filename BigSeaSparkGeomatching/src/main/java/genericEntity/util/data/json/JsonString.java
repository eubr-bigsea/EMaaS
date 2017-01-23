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
 * <code>JsonString</code> represents a Json-convertible String.
 * 
 * @author Matthias Pohl
 */
public class JsonString implements JsonAtomic, Serializable {

	private static final Logger logger = Logger.getLogger(JsonString.class.getPackage().getName());

	private String strValue;

	/**
	 * Initializes an empty Json-convertible String.
	 */
	public JsonString() {
		this("");
	}

	/**
	 * Initializes a new Json-convertible String.
	 * 
	 * @param value
	 *            The actual String value.
	 */
	public JsonString(String value) {
		this.strValue = value;
	}

	/**
	 * Creates a <code>JsonString</code> instance based on the passed Json code.
	 * 
	 * @param jsonCode
	 *            The code that shall be converted into a <code>JsonString</code>
	 * @return The newly created <code>JsonString</code>.
	 * @throws ParseException
	 *             If the passed Json code cannot be converted into a <code>JsonString</code>.
	 */
	public static JsonString createJsonString(String jsonCode) throws ParseException {
		JsonString str = new JsonString();
		try {
			str.fromJson(new GenericJsonParser<JsonValue>(JsonValue.class, jsonCode));
		} catch (IOException e) {
			JsonString.logger.error("IOException occurred within createJsonString(String) when processing \"" + jsonCode + "\".");
		}
		
		return str;
	}

	/**
	 * Returns <code>JsonType.String</code>.
	 * 
	 * @see JsonValue#getType()
	 */
	@Override
	public JsonType getType() {
		return JsonValue.JsonType.String;
	}

	/**
	 * Converts the passed String into its Json representation.
	 * 
	 * @param str
	 *            The String that shall be converted.
	 * @return The Json representation of the passed String.
	 */
	public static String jsonize(String str) {
		char[] charArray = str.toCharArray();

		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append('"');
		for (int i = 0; i < charArray.length; i++) {
			// escapes all escape characters and quote characters within the String
			switch (charArray[i]) {
			case '\\':
			case '"':
				strBuilder.append('\\');
				strBuilder.append(charArray[i]);
				break;
			case '\t':
				strBuilder.append('\\');
				strBuilder.append('t');
				break;
			case '\n':
				strBuilder.append('\\');
				strBuilder.append('n');
				break;
			case '\r':
				strBuilder.append('\\');
				strBuilder.append('r');
				break;
			case '\f':
				strBuilder.append('\\');
				strBuilder.append('f');
				break;
			case '\b':
				strBuilder.append('\\');
				strBuilder.append('b');
				break;
			default:
				strBuilder.append(charArray[i]);
			}
		}

		strBuilder.append('"');

		return strBuilder.toString();
	}

	@Override
	public String getStringValue() {
		return this.strValue;
	}

	/**
	 * Returns the actual String.
	 * 
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return this.getStringValue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.strValue == null) ? 0 : this.strValue.hashCode());
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
		JsonString other = (JsonString) obj;
		if (this.strValue == null) {
			if (other.strValue != null) {
				return false;
			}
		} else if (!this.strValue.equals(other.strValue)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns <code>1</code> since <code>JsonString</code> is an atomic value.
	 * 
	 * @see JsonValue#size()
	 */
	@Override
	public int size() {
		return 1;
	}

	@Override
	public int compareTo(JsonValue other) {
		if (other == null) {
			return 1;
		} else if (this == other) {
			return 0;
		} else if (this.getType().compareTo(other.getType()) != 0) {
			return this.getType().compareTo(other.getType());
		}

		return this.strValue.compareTo(((JsonString) other).strValue);
	}

	@Override
	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeJsonString(this);
	}
	
	@Override
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		this.strValue = jsonParser.nextJsonString().strValue;
	}

}
