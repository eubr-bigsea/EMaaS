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
 * <code>JsonBoolean</code> represents a boolean value that can be converted into Json.
 * 
 * @author Matthias Pohl
 */
public class JsonBoolean implements JsonAtomic {

	private boolean boolValue;

	/**
	 * A Json-convertible <code>true</code>.
	 */
	public static final JsonBoolean TRUE = new JsonBoolean(true);

	/**
	 * A Json-convertible <code>false</code>.
	 */
	public static final JsonBoolean FALSE = new JsonBoolean(false);

	private JsonBoolean(boolean value) {
		this.boolValue = value;
	}

	private JsonBoolean() {
		this.boolValue = true;
	}

	/**
	 * Returns a Json-convertible boolean based on the passed String.
	 * 
	 * @param jsonCode
	 *            The String that shall be converted into a Json-convertible boolean.
	 * @return {@link JsonBoolean#TRUE} if and only if <code>jsonCode</code> is the String "true" (case-insensitive); otherwise
	 *         {@link JsonBoolean#FALSE}.
	 */
	public static JsonBoolean createJsonBoolean(String jsonCode) {
		if ("true".equalsIgnoreCase(jsonCode)) {
			return JsonBoolean.TRUE;
		}

		return JsonBoolean.FALSE;
	}

	/**
	 * Returns the Json representation of the passed boolean value.
	 * 
	 * @param boolVal
	 *            The boolean value whose Json representation shall be returned.
	 * @return Returns {@link #TRUE} or {@link #FALSE} depending on the passed boolean value.
	 */
	public static JsonBoolean createJsonBoolean(boolean boolVal) {
		return boolVal ? JsonBoolean.TRUE : JsonBoolean.FALSE;
	}

	/**
	 * Returns <code>JsonType.Boolean</code>.
	 * 
	 * @see JsonValue#getType()
	 */
	@Override
	public JsonType getType() {
		return JsonValue.JsonType.Boolean;
	}

	@Override
	public String toString() {
		return this.boolValue ? "true" : "false";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.boolValue ? 1231 : 1237);
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
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		JsonBoolean other = (JsonBoolean) obj;
		if (this.boolValue != other.boolValue) {
			return false;
		}
		return true;
	}

	/**
	 * Returns <code>1</code> since <code>JsonBoolean</code> is an atomic value.
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

		if (this.boolValue && !((JsonBoolean) other).boolValue) {
			return 1;
		} else if (!this.boolValue && ((JsonBoolean) other).boolValue) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Returns the actual value of this <code>JsonBoolean</code>.
	 * 
	 * @return The actual value of this <code>JsonBoolean</code>.
	 */
	public boolean getValue() {
		return this.boolValue;
	}

	@Override
	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeJsonBoolean(this);
	}

	@Override
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		this.boolValue = jsonParser.nextJsonBoolean().boolValue;
	}

	@Override
	public String getStringValue() {
		if (this.boolValue) {
			return "true";
		}
		
		return "false";
	}

}
