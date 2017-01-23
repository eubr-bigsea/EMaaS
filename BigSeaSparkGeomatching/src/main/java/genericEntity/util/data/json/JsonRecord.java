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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

/**
 * <code>JsonRecord</code> represents a Json record.
 * 
 * @author Matthias Pohl
 */
public class JsonRecord extends HashMap<String, JsonValue> implements JsonValue {

	private static final Logger logger = Logger.getLogger(JsonRecord.class.getPackage().getName());

	/**
	 * Auto-generated serial id which is needed when extending {@link HashMap}.
	 */
	private static final long serialVersionUID = 2181219319110875808L;

	/**
	 * Initializes a <code>JsonRecord</code>.
	 */
	public JsonRecord() {
		super();
	}

	/**
	 * Initializes a <code>JsonRecord</code> with the passed initial capacity.
	 * 
	 * @param initialCapacity
	 *            The initial capacity of the record.
	 */
	public JsonRecord(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Initializes a new <code>JsonRecord</code> with the passed data.
	 * 
	 * @param initialData
	 *            The data that will be added during the initialization.
	 */
	public JsonRecord(Map<? extends String, ? extends JsonValue> initialData) {
		super(initialData);
	}

	/**
	 * Initializes a <code>JsonRecord</code> with the passed initial capacity and its load factor.
	 * 
	 * @param initialCapacity
	 *            The initial capacity of the record.
	 * @param loadFactor
	 *            The load factor of the record.
	 * 
	 * @see HashMap#HashMap(int, float)
	 */
	public JsonRecord(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a <code>JsonRecord</code> instance based on the passed Json code.
	 * 
	 * @param jsonCode
	 *            The code that shall be converted into a <code>JsonRecord</code>
	 * @return The newly created <code>JsonRecord</code>.
	 * @throws ParseException
	 *             If the passed Json code cannot be converted into a <code>JsonRecord</code>.
	 */
	public static JsonRecord createJsonRecord(String jsonCode) throws ParseException {
		final JsonRecord record = new JsonRecord();
		try {
			record.fromJson(new GenericJsonParser<JsonValue>(JsonValue.class, jsonCode));
		} catch (IOException e) {
			JsonRecord.logger.error("IOException occurred within createJsonRecord(String) when processing \"" + jsonCode + "\".");
		}

		return record;
	}

	/**
	 * Returns <code>JsonType.Record</code>.
	 * 
	 * @see JsonValue#getType()
	 */
	@Override
	public JsonType getType() {
		return JsonValue.JsonType.Record;
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

		JsonRecord otherRecord = (JsonRecord) other;

		// empty records are the "smallest" records
		if (otherRecord.size() == 0 && this.size() == 0) {
			return 0;
		} else if (otherRecord.size() == 0 && this.size() > 0) {
			return 1;
		} else if (this.size() == 0 && otherRecord.size() > 0) {
			return -1;
		}

		// generate sorted key sets
		Iterator<String> keySetIterator1 = new TreeSet<String>(this.keySet()).iterator();
		Iterator<String> keySetIterator2 = new TreeSet<String>(otherRecord.keySet()).iterator();

		String currentKey1 = "";
		String currentKey2 = "";
		do {
			currentKey1 = keySetIterator1.next();
			currentKey2 = keySetIterator2.next();

			// keys are equal -> compare their values
			if (currentKey1.equals(currentKey2)) {
				// values are unequal -> return comparison value; otherwise go
				// on
				int cmpValue = this.get(currentKey1).compareTo(otherRecord.get(currentKey2));
				if (cmpValue != 0) {
					return cmpValue;
				}
			} else {
				// if both keys are unequal, compare records based on these keys
				return currentKey1.compareTo(currentKey2);
			}
		} while (keySetIterator1.hasNext() && keySetIterator2.hasNext());

		if (keySetIterator1.hasNext() && !keySetIterator2.hasNext()) {
			// this record has more keys -> it is "larger" than the other one
			return 1;
		} else if (!keySetIterator1.hasNext() && keySetIterator2.hasNext()) {
			// this record has less keys -> it is "smaller" than the other one
			return -1;
		} else {
			// both have no keys left -> both are equal
			return 0;
		}
	}

	/**
	 * Returns the first occurrence of the passed attribute (using a recursive look-up).
	 * 
	 * @param attributeName
	 *            The name of the attribute whose value is requested.
	 * @return The value of the attribute or <code>null</code> if such an attribute was not found.
	 */
	public JsonValue searchFor(String attributeName) {
		if (attributeName == null) {
			return null;
		}

		if (this.containsKey(attributeName)) {
			return this.get(attributeName);
		}

		Iterator<String> keyIterator = this.keySet().iterator();

		while (keyIterator.hasNext()) {
			String currentKey = keyIterator.next();
			if (this.get(currentKey).getType() == JsonType.Array) {
				return ((JsonArray) this.get(currentKey)).getJsonValue(attributeName);
			} else if (this.get(currentKey).getType() == JsonType.Record) {
				return ((JsonRecord) this.get(currentKey)).searchFor(attributeName);
			}
		}

		// no element found
		return null;
	}

	/**
	 * Returns the {@link JsonValue} specified by the passed path or <code>null</code> if the specified attribute does not exist.
	 * 
	 * @param path
	 *            The path to the attribute.
	 * @return The corresponding <code>JsonValue</code>.
	 */
	public JsonValue getJsonValue(String... path) {
		if (path == null || path.length == 0) {
			return null;
		}

		Iterator<String> pathIterator = Arrays.asList(path).iterator();
		return this.getJsonValue(pathIterator.next(), pathIterator);
	}

	/**
	 * Returns the value that corresponds to the passed path.
	 * 
	 * @param currentAttributeName
	 *            The currently observed attribute within the record. If this attribute does not exist, <code>null</code> will be returned.
	 * @param path
	 *            The rest of the path.
	 * @return The corresponding value or <code>null</code>, if the path is invalid.
	 */
	JsonValue getJsonValue(String currentAttributeName, Iterator<String> path) {
		if (!this.containsKey(currentAttributeName)) {
			JsonRecord.logger.debug("The following attribute was not found: " + currentAttributeName);
			return null;
		}

		JsonValue currentValue = this.get(currentAttributeName);

		if (!path.hasNext()) {
			return currentValue;
		}

		if (currentValue.getType() == JsonType.Record) {
			return ((JsonRecord) currentValue).getJsonValue(path.next(), path);
		} else if (currentValue.getType() == JsonType.Array) {
			return ((JsonArray) currentValue).getJsonValue(path.next(), path);
		} else {
			JsonRecord.logger.debug("Current value for '" + currentAttributeName
					+ "' is no record or array. The requested path could not be traversed.");
			return null;
		}
	}

	/**
	 * Generates a concatenated String out of all values stored in this <code>JsonRecord</code>. This method should be used instead of
	 * {@link #toString()} if the enclosed values should be concatenated without any separators and key information.
	 * 
	 * @return A concatenation of a all values stored in this <code>JsonRecord</code>.
	 */
	public String generateString() {
		StringBuilder strBuilder = new StringBuilder();
		for (Map.Entry<String, JsonValue> entry : this.entrySet()) {
			if (strBuilder.length() > 0) {
				strBuilder.append(' ');
			}

			final JsonValue value = entry.getValue();
			switch (value.getType()) {
			case Array:
				strBuilder.append(((JsonArray) value).generateString());
				break;
			case Record:
				strBuilder.append(((JsonRecord) value).generateString());
				break;
			case Null:
				// ignore value
				break;
			case String:
			case Number:
			case Boolean:
			default:
				strBuilder.append(value.toString());
				break;
			}
		}

		return strBuilder.toString();
	}

	/**
	 * Puts the passed key and {@link JsonValue} into the <code>JsonRecord</code>. If the passed <code>value</code> is <code>null</code>,
	 * <code>JsonNull</code> will be inserted instead.
	 * 
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	@Override
	public JsonValue put(String key, JsonValue value) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		if (value == null) {
			return this.putNull(key);
		}

		return super.put(key, value);
	}

	/**
	 * Puts the passed key and {@link JsonNull} into the <code>JsonRecord</code> .
	 * 
	 * @param key
	 *            The key which shall be added with <code>JsonNull</code>.
	 * 
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * 
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue putNull(String key) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		return super.put(key, JsonNull.NULL);
	}

	/**
	 * Puts the passed key and the corresponding {@link JsonBoolean} value into the <code>JsonRecord</code>. If the passed <code>value</code> is
	 * <code>null</code>, <code>JsonNull</code> will be inserted instead.
	 * 
	 * @param key
	 *            The key that shall be added.
	 * @param value
	 *            The boolean value whose Json representation shall be put into the <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue put(String key, boolean value) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		if (value) {
			return super.put(key, JsonBoolean.TRUE);
		}

		return super.put(key, JsonBoolean.FALSE);
	}

	/**
	 * Puts a {@link JsonArray} generated out of the passed <code>Collection</code> and its key to this <code>JsonRecord</code>. If the passed value
	 * is <code>null</code>, {@link JsonNull} will be used instead.
	 * 
	 * @param key
	 *            The key that shall be put into the <code>JsonRecord</code>.
	 * @param collection
	 *            The <code>Collection</code> that shall be converted into a <code>JsonArray</code> and put into the <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue putCollection(String key, Collection<? extends JsonValue> collection) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		if (collection == null) {
			return this.putNull(key);
		}

		return super.put(key, new JsonArray(collection));
	}

	/**
	 * Puts the passed key and the corresponding {@link JsonNumber} value into the <code>JsonRecord</code>. If the passed <code>value</code> is
	 * <code>null</code>, <code>JsonNull</code> will be inserted instead.
	 * 
	 * @param key
	 *            The key that shall be added.
	 * @param value
	 *            The integer value whose Json representation shall be put into the <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue put(String key, int value) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		return super.put(key, new JsonNumber(Integer.valueOf(value)));
	}

	/**
	 * Puts the passed key and the corresponding {@link JsonNumber} value into the <code>JsonRecord</code>. If the passed <code>value</code> is
	 * <code>null</code>, <code>JsonNull</code> will be inserted instead.
	 * 
	 * @param key
	 *            The key that shall be added.
	 * @param value
	 *            The long value whose Json representation shall be put into the <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue put(String key, long value) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		return super.put(key, new JsonNumber(Long.valueOf(value)));
	}

	/**
	 * Puts the passed key and the corresponding {@link JsonNumber} value into the <code>JsonRecord</code>. If the passed <code>value</code> is
	 * <code>null</code>, <code>JsonNull</code> will be inserted instead.
	 * 
	 * @param key
	 *            The key that shall be added.
	 * @param value
	 *            The double value whose Json representation shall be put into the <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue put(String key, double value) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		return super.put(key, new JsonNumber(Double.valueOf(value)));
	}

	/**
	 * Puts a <code>JsonRecord</code> generated out of the passed <code>Map</code> and its key to this <code>JsonRecord</code>. If the passed value is
	 * <code>null</code>, {@link JsonNull} will be used instead.
	 * 
	 * @param key
	 *            The key that shall be put into the <code>JsonRecord</code>.
	 * @param map
	 *            The <code>Map</code> that shall be converted into a <code>JsonRecord</code> and put into this <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue putMap(String key, Map<String, ? extends JsonValue> map) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		if (map == null) {
			return this.putNull(key);
		}

		return super.put(key, new JsonRecord(map));
	}

	/**
	 * Puts the passed key and the corresponding {@link JsonString} value into the <code>JsonRecord</code>. If the passed <code>value</code> is
	 * <code>null</code>, <code>JsonNull</code> will be inserted instead.
	 * 
	 * @param key
	 *            The key that shall be added.
	 * @param str
	 *            The String whose Json representation shall be put into the <code>JsonRecord</code>.
	 * @return The previous value associated with the passed key, or <code>null</code> if there was no mapping for this key.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a key.
	 * 
	 * @see HashMap#put(Object, Object)
	 */
	public JsonValue put(String key, String str) {
		if (key == null) {
			throw new NullPointerException("The key needs to be specified.");
		}

		if (str == null) {
			return this.putNull(key);
		}

		return super.put(key, new JsonString(str));
	}

	@Override
	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeJsonRecord(this);
	}

	@Override
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		this.clear();
		this.putAll(jsonParser.nextJsonRecord());
	}

}
