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

package genericEntity.datasource;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import genericEntity.exception.ExtractionFailedException;
import genericEntity.util.AbstractCleanable;
import genericEntity.util.AbstractIterator;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.JsonArray;
import genericEntity.util.data.json.JsonNumber;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.json.JsonValue;
import genericEntity.util.data.json.JsonValue.JsonType;

/**
 * <code>AbstractDataSource</code> provides the common functionality of all {@link DataSource} classes. The concrete <code>DataSource</code>
 * implementations should extend this class in order to reduce the implementation effort.
 * 
 * @author Matthias Pohl
 * @author Arvid Heise
 * 
 * @param <S>
 *            Type of subclass for fluent interface.
 * 
 * @see DataSource
 * @see AbstractIterator
 */
public abstract class AbstractDataSource<S extends AbstractDataSource<S>> extends AbstractCleanable implements DataSource {

	/**
	 * <code>AbstractDataSourceIterator</code> can be used to generate valid {@link GenericObject}s.
	 * 
	 * @author Matthias Pohl
	 * 
	 * @param <T>
	 *            Internal used {@link AbstractDataSource}.
	 */
	protected abstract class AbstractDataSourceIterator<T extends AbstractDataSource<?>> extends AbstractIterator<GenericObject> {

		/**
		 * The internally used {@link DataSource}.
		 */
		protected final T dataSource;

		private int objectCount = 0;

		/**
		 * Initializes an <code>AbstractDataSourceIterator</code> with for the passed {@link AbstractDataSource}.
		 * 
		 * @param source
		 *            The <code>AbstractDataSource</code> of which the data shall be returned.
		 */
		protected AbstractDataSourceIterator(T source) {
			if (source == null) {
				throw new NullPointerException("The source must be set.");
			}

			this.dataSource = source;
		}

		private int nextObjectId() {
			return this.objectCount++;
		}

		/**
		 * Adds a {@link JsonValue} instance to an attribute of the passed {@link JsonRecord}. If this attribute has already a value, all the values
		 * are combined in a {@link JsonArray}.
		 * 
		 * @param record
		 *            The record to which the value shall be added.
		 * @param attributeName
		 *            The attribute of which the value shall be added.
		 * @param attributeValue
		 *            The value that shall be added.
		 * @return The modified record.
		 */
		protected JsonRecord addAttributeValue(JsonRecord record, String attributeName, JsonValue attributeValue) {
			if (record.containsKey(attributeName)) {
				JsonValue oldValue = record.get(attributeName);
				JsonArray arr;

				if (oldValue.getType() == JsonType.Array) {
					arr = (JsonArray) oldValue;
				} else {
					arr = new JsonArray(oldValue);
				}

				arr.add(attributeValue);
				record.put(attributeName, arr);
			} else {
				record.put(attributeName, attributeValue);
			}

			return record;
		}

		/**
		 * Sets a {@link JsonValue} instance to an attribute of the passed {@link JsonRecord}. If this attribute has already a value, this value will
		 * be overwritten.
		 * 
		 * @param record
		 *            The record to which the value shall be added.
		 * @param attributeName
		 *            The attribute of which the value shall be set.
		 * @param attributeValue
		 *            The value that shall be set.
		 * @return The modified record.
		 */
		protected JsonRecord setAttributeValue(JsonRecord record, String attributeName, JsonValue attributeValue) {
			record.put(attributeName, attributeValue);
			return record;
		}

		/**
		 * Generates a {@link GenericObject} based on the data returned by {@link #loadNextRecord()}.
		 */
		@Override
		protected GenericObject loadNextElement() throws ExtractionFailedException {
			final JsonRecord currentObjectData = this.loadNextRecord();

			if (currentObjectData == null) {
				return null;
			}

			final JsonArray objectId = new JsonArray();

			final int currentObjectsId = this.nextObjectId();

			// key generation
			if (!this.dataSource.autoGeneratedIds()) {
				// put the values of every id column into the array
				for (final String idColName : this.dataSource.getIdAttributes()) {
					if (!currentObjectData.containsKey(idColName)) {
						AbstractDataSource.logger.warn("'" + idColName + "' was not found in record.");
					} else {
						objectId.add(currentObjectData.get(idColName));
					}
				}
			} else {
				// if no primary key column is selected -> object identifier is generated automatically
				objectId.add(new JsonNumber(currentObjectsId));
			}

			// currentObjectsId starts with 0
			this.dataSource.setExtractedRecordCount(currentObjectsId + 1);

			return new GenericObject(currentObjectData, this.dataSource.getIdentifier(), objectId);
		}

		/**
		 * Returns the data of the next object. This method will be called within {@link #loadNextElement()}.
		 * 
		 * @return The data record of the next object.
		 * @throws ExtractionFailedException
		 *             If an error occurs while extracting the data.
		 */
		protected abstract JsonRecord loadNextRecord() throws ExtractionFailedException;
	}

	private static final Logger logger = Logger.getLogger(AbstractDataSource.class.getPackage().getName());

	private String identifier;
	private Collection<String> idAttributes = new HashSet<String>();

	private transient Integer extractedRecordCount = 0;

	/**
	 * Internal constructor for {@link Jsonable} deserialization.
	 */
	protected AbstractDataSource() {
		// nothing to do
	}

	/**
	 * Initializes a <code>AbstractDataExtractor</code> with the passed identifier.
	 * 
	 * @param id
	 *            The identifier that is used within each generated {@link GenericObject} as its source identifier.
	 */
	protected AbstractDataSource(String id) {
		if (id == null) {
			throw new NullPointerException();
		}

		this.identifier = id;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public void addIdAttributes(String... attributes) {
		for (String attribute : attributes) {
			if (attribute == null) {
				AbstractDataSource.logger.warn("Attribute ignored: null was passed as a id attribute.");
			} else {
				this.idAttributes.add(attribute);
			}
		}
	}

	/**
	 * Sets the id attributes.
	 * 
	 * @param attributes
	 *            The id attributes which shall be used.
	 * @return The current instance.
	 * 
	 * @see #addIdAttributes(String...)
	 */
	@SuppressWarnings("unchecked")
	public S withIdAttributes(String... attributes) {
		this.addIdAttributes(attributes);
		return (S) this;
	}

	/**
	 * Checks whether the object id is generated automatically. This method indicates whether id attributes are set or not. If no id attributes are
	 * set, the id have to be generated automatically by incrementing an Integer value. Otherwise the values of the id attributes are used to generate
	 * the object id of each extracted record.
	 * 
	 * @return <code>true</code>, if no id attributes are set; otherwise <code>false</code>.
	 */
	protected boolean autoGeneratedIds() {
		return this.idAttributes.isEmpty();
	}

	/**
	 * Returns the set id attributes.
	 * 
	 * @return The set id attributes.
	 */
	protected Iterable<String> getIdAttributes() {
		return this.idAttributes;
	}

	@Override
	public abstract Iterator<GenericObject> iterator();

	/**
	 * Resets the extracted-record count, if the passed count is larger than the current one.
	 * 
	 * @param extractedRecordCnt
	 *            The new record count. If this value is not larger than the current one, no reset is performed.
	 */
	protected void setExtractedRecordCount(int extractedRecordCnt) {
		this.extractedRecordCount = Math.max(this.extractedRecordCount, extractedRecordCnt);
	}

	@Override
	public int getExtractedRecordCount() {
		return this.extractedRecordCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.idAttributes == null) ? 0 : this.idAttributes.hashCode());
		result = prime * result + ((this.identifier == null) ? 0 : this.identifier.hashCode());
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

//		if (this.getClass() != obj.getClass()) {
//			return false;
//		}

		@SuppressWarnings("unchecked")
		AbstractDataSource<S> other = (AbstractDataSource<S>) obj;
		if (this.idAttributes == null) {
			if (other.idAttributes != null) {
				return false;
			}
		} else if (!this.idAttributes.equals(other.idAttributes)) {
			return false;
		}

		if (this.identifier == null) {
			if (other.identifier != null) {
				return false;
			}
		} else if (!this.identifier.equals(other.identifier)) {
			return false;
		}

		return true;
	}

	@Override
	public void close() throws IOException {
		this.cleanUp();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataSource [identifier=").append(this.identifier).append(']');
		return builder.toString();
	}

}
