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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

/**
 * <code>JsonArray</code> represents an ordered collection and provides functionality for collecting multiple instances
 * of {@link JsonValue}.
 * 
 * @author Matthias Pohl
 */
public class JsonArray extends ArrayList<JsonValue> implements JsonValue {

	private static final Logger logger = Logger.getLogger(JsonArray.class.getPackage().getName());

	private static final long serialVersionUID = 1L;

	/**
	 * Initializes a new <code>JsonArray</code>.
	 */
	public JsonArray() {
		super();
	}

	/**
	 * Initializes a new <code>JsonArray</code> with the passed {@link JsonValue}.
	 * 
	 * @param initialContent
	 *            The <code>JsonValues</code> that shall be added to the <code>JsonArray</code> initially.
	 */
	public JsonArray(JsonValue... initialContent) {
		super();
		this.addAll(Arrays.asList(initialContent));
	}

	/**
	 * Initializes a new <code>JsonArray</code> with the passed data.
	 * 
	 * @param initialData
	 *            The data that will be added during the initialization.
	 */
	public JsonArray(Collection<? extends JsonValue> initialData) {
		super(initialData);
	}

	/**
	 * Initializes a new <code>JsonArray</code> with a predefined capacity.
	 * 
	 * @param initialCapacity
	 *            The predefined capacity.
	 */
	public JsonArray(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a <code>JsonArray</code> instance based on the passed Json code.
	 * 
	 * @param jsonCode
	 *            The code that shall be converted into a <code>JsonArray</code>
	 * @return The newly created <code>JsonArray</code>.
	 * @throws ParseException
	 *             If the passed Json code cannot be converted into a <code>JsonArray</code>.
	 */
	public static JsonArray createJsonArray(String jsonCode) throws ParseException {
		final JsonArray array = new JsonArray();
		try {
			array.fromJson(new GenericJsonParser<JsonValue>(JsonValue.class, jsonCode));
		} catch (IOException e) {
			JsonArray.logger.error("IOException occurred within createJsonArray(String) when processing \"" + jsonCode
					+ "\".");
		}
		
		return array;
	}

	/**
	 * Returns <code>JsonType.Array</code>.
	 * 
	 * @see JsonValue#getType()
	 */
	public JsonType getType() {
		return JsonValue.JsonType.Array;
	}

	public int compareTo(JsonValue other) {
		if (other == null) {
			return 1;
		} else if (this == other) {
			return 0;
		} else if (this.getType().compareTo(other.getType()) != 0) {
			return this.getType().compareTo(other.getType());
		}

		JsonArray otherArray = (JsonArray) other;
		
		// empty arrays are the "smallest" arrays
		if (otherArray.size() == 0 && this.size() == 0) {
			return 0;
		} else if (otherArray.size() == 0 && this.size() > 0) {
			return 1;
		} else if (this.size() == 0 && otherArray.size() > 0) {
			return -1;
		}

		if (this.size() < otherArray.size()) {
			Iterator<JsonValue> otherArrayIterator = otherArray.iterator();
			for (JsonValue value : this) {
				int cmpValue = value.compareTo(otherArrayIterator.next());
				if (cmpValue != 0) {
					return cmpValue;
				}
			}
		} else {
			Iterator<JsonValue> thisArrayIterator = this.iterator();
			for (JsonValue value : otherArray) {
				int cmpValue = thisArrayIterator.next().compareTo(value);
				if (cmpValue != 0) {
					return cmpValue;
				}
			}
		}

		return 0;
	}

	/**
	 * Returns the first occurrence of the passed attribute (using a recursive look-up). This is only possible if the
	 * array contains {@link JsonRecord}s.
	 * 
	 * @param attributeName
	 *            The name of the attribute whose value is requested.
	 * @return The value of the attribute or <code>null</code> if such an attribute was not found.
	 */
	public JsonValue getJsonValue(String attributeName) {
		if (attributeName == null) {
			return null;
		}

		Iterator<JsonValue> valueIterator = this.iterator();

		while (valueIterator.hasNext()) {
			JsonValue currentValue = valueIterator.next();
			if (currentValue.getType() == JsonType.Array) {
				return ((JsonArray) currentValue).getJsonValue(attributeName);
			} else if (currentValue.getType() == JsonType.Record) {
				return ((JsonRecord) currentValue).searchFor(attributeName);
			}
		}

		// no element found
		return null;
	}

	/**
	 * Iterates over the elements and looks for a {@link JsonRecord} that contains <code>currentAttributeName</code>.
	 * 
	 * @param currentAttributeName
	 *            The current part of the path that is traversed.
	 * @param path
	 *            The rest of the path that needs to be traversed.
	 * @return The actual value that corresponds to the passed path or <code>null</code>, if the specified attribute
	 *         couldn't be found.
	 */
	JsonValue getJsonValue(String currentAttributeName, Iterator<String> path) {
		for (JsonValue val : this) {
			if (val.getType() == JsonType.Record) {
				return ((JsonRecord) val).getJsonValue(currentAttributeName, path);
			} else if (val.getType() == JsonType.Array) {
				return ((JsonArray) val).getJsonValue(currentAttributeName, path);
			}
		}

		return null;
	}

	/**
	 * Generates a concatenated String out of all values stored in this <code>JsonArray</code>. This method should be
	 * used instead of {@link #toString()} if the enclosed values should be concatenated without any separators.
	 * 
	 * @return A concatenation of a all values stored in this <code>JsonArray</code>.
	 */
	public String generateString() {
		StringBuilder strBuilder = new StringBuilder();
		for (JsonValue val : this) {
			if (strBuilder.length() > 0) {
				strBuilder.append(' ');
			}
			switch (val.getType()) {
			case Array:
				strBuilder.append(((JsonArray) val).generateString());
				break;
			case Record:
				strBuilder.append(((JsonRecord) val).generateString());
				break;
			case Null:
				// ignore value
				break;
			case String:
			case Number:
			case Boolean:
			default:
				strBuilder.append(val.toString());
				break;
			}
		}

		return strBuilder.toString();
	}

	/**
	 * Adds the {@link JsonValue} to the end of the <code>JsonArray</code>. If the passed value is <code>null</code>,
	 * {@link JsonNull} will be added.
	 * 
	 * @see ArrayList#add(Object)
	 */
	@Override
	public boolean add(JsonValue value) {
		if (value == null) {
			return super.add(JsonNull.NULL);
		}

		return super.add(value);
	}

	/**
	 * Adds {@link JsonNull} to the end of the <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean addNull() {
		return super.add(JsonNull.NULL);
	}

	/**
	 * Adds a <code>JsonArray</code> generated out of the passed <code>Collection</code> to the end of the
	 * <code>JsonArray</code>. If the passed value is <code>null</code>, {@link JsonNull} will be added. This method is
	 * different from {@link #addAll(Collection)} which adds all elements of the passed collection to this one!
	 * 
	 * @param collection
	 *            The collection that shall be converted into a <code>JsonArray</code> and added to this
	 *            <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean addCollection(Collection<? extends JsonValue> collection) {
		if (collection == null) {
			return super.add(JsonNull.NULL);
		}

		return super.add(new JsonArray(collection));
	}

	/**
	 * Adds a {@link JsonBoolean} to the end of the <code>JsonArray</code>.
	 * 
	 * @param value
	 *            The actual boolean value that shall be converted into its corresponding Json representation and added
	 *            to this <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean add(boolean value) {
		if (value) {
			return super.add(JsonBoolean.TRUE);
		}

		return super.add(JsonBoolean.FALSE);
	}

	/**
	 * Adds a {@link JsonNumber} to the end of the <code>JsonArray</code>.
	 * 
	 * @param value
	 *            The actual integer value that shall be converted into its corresponding Json representation and added
	 *            to this <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean add(int value) {
		return super.add(new JsonNumber(Integer.valueOf(value)));
	}

	/**
	 * Adds a {@link JsonNumber} to the end of the <code>JsonArray</code>.
	 * 
	 * @param value
	 *            The actual long value that shall be converted into its corresponding Json representation and added to
	 *            this <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean add(long value) {
		return super.add(new JsonNumber(Long.valueOf(value)));
	}

	/**
	 * Adds a {@link JsonNumber} to the end of the <code>JsonArray</code>.
	 * 
	 * @param value
	 *            The actual double value that shall be converted into its corresponding Json representation and added
	 *            to this <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean add(double value) {
		return super.add(new JsonNumber(Double.valueOf(value)));
	}

	/**
	 * Adds a {@link JsonRecord} generated out of the passed <code>Map</code> to the end of the <code>JsonArray</code>. If the passed value is <code>null</code>,
	 * {@link JsonNull} will be added.
	 * 
	 * @param map
	 *            The map that shall be converted into a <code>JsonRecord</code> and added to this
	 *            <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean addMap(Map<String, ? extends JsonValue> map) {
		if (map == null) {
			return super.add(JsonNull.NULL);
		}

		return super.add(new JsonRecord(map));
	}

	/**
	 * Adds a {@link JsonString} to the end of the <code>JsonArray</code>. If the passed value is <code>null</code>,
	 * {@link JsonNull} will be added.
	 * 
	 * @param str
	 *            The actual String value that shall be converted into its corresponding Json representation and added
	 *            to this <code>JsonArray</code>.
	 * 
	 * @return If the content of this <code>JsonArray</code> changed as a result of this call; otherwise
	 *         <code>false</code>.
	 * 
	 * @see ArrayList#add(Object)
	 */
	public boolean add(String str) {
		if (str == null) {
			return super.add(JsonNull.NULL);
		}

		return super.add(new JsonString(str));
	}

	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeJsonArray(this);
	}
	
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		this.clear();
		this.addAll(jsonParser.nextJsonArray());
	}

}
