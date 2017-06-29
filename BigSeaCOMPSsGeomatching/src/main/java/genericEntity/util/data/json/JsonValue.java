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

import genericEntity.util.data.Jsonable;

/**
 * <code>JsonValue</code> provides methods that has to be implemented by every Json data type.
 * 
 * @author Matthias Pohl
 */
public interface JsonValue extends Jsonable, Comparable<JsonValue> {

	/**
	 * <code>JsonValue.JsonType</code> includes all Json types that can be returned by {@link JsonValue#getType()}.
	 * 
	 * @author Matthias Pohl
	 */
	public enum JsonType {
		/**
		 * <code>null</code>.
		 */
		Null(true),
		/**
		 * The type of an array.
		 */
		Array(false),
		/**
		 * The type of a boolean value.
		 */
		Boolean(true),
		/**
		 * The type of a numerical value.
		 */
		Number(true),
		/**
		 * The type of a Json record.
		 */
		Record(false),
		/**
		 * The type of a String value.
		 */
		String(true);

		private final boolean isAtomic;

		private JsonType(boolean isAtomic) {
			this.isAtomic = isAtomic;
		}

		/**
		 * Checks, whether the current type is atomic.
		 * 
		 * @return <code>true</code>, if the instance does not collect several other Json instances; otherwise <code>false</code>.
		 */
		public boolean isAtomic() {
			return this.isAtomic;
		}
	}

	/**
	 * Returns the type of the current instance.
	 * 
	 * @return The type of the current instance.
	 */
	public JsonType getType();

	/**
	 * The size of the current instance. The size is the number of actual Java instances that are included in this Json instance.
	 * 
	 * @return The number of concrete Java objects that are collected by the current instance.
	 */
	public int size();

	/**
	 * Compares the current instance with the passed {@link JsonValue}. Instances of different {@link JsonType}s are ordered lexicographically except
	 * for {@link JsonType#Null}. The above mentioned requirements define the following order: <code>null</code> == {@link JsonType#Null} &lt;
	 * {@link JsonType#Array} &lt; {@link JsonType#Boolean} &lt; {@link JsonType#Number} &lt; {@link JsonType#Record} &lt; {@link JsonType#String}.
	 * 
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	public int compareTo(JsonValue value);
}
