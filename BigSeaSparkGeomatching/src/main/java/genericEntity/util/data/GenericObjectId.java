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

package genericEntity.util.data;

import java.io.IOException;
import java.io.Serializable;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;
import genericEntity.util.data.json.JsonArray;
import genericEntity.util.data.json.JsonString;

/**
 * <code>GenericObjectId</code> encapsulates the identifying information of each {@link GenericObject}.
 * 
 * @author Matthias Pohl
 */
public class GenericObjectId implements Jsonable, Comparable<GenericObjectId>, Serializable {

	/**
	 * The attribute name of the object id within the Json representation of this <code>GenericObject</code>.
	 */
	public final static String OBJECT_ID_ATTRIBUTE_NAME = "objectId";

	/**
	 * The attribute name of the source id within the Json representation of this <code>GenericObject</code>.
	 */
	public final static String SOURCE_ID_ATTRIBUTE_NAME = "sourceId";

	private String sourceId = null;
	private JsonArray objectId = null;

	/**
	 * Initializes an invalid <code>GenericObjectId</code>.
	 */
	GenericObjectId() {
		this(null, null);
	}

	/**
	 * Initializes a <code>GenericObjectId</code> with the passed identifiers. If one of the parameters is
	 * <code>null</code>, the id will be <code>invalid</code>.
	 * 
	 * @param srcId
	 *            The identifier of the corresponding source.
	 * @param objId
	 *            The actual identifier of the object.
	 */
	GenericObjectId(String srcId, JsonArray objId) {
		this.sourceId = srcId;
		this.objectId = objId;
	}

	/**
	 * Returns the source identifier.
	 * 
	 * @return The source identifier or <code>null</code>, if the id is <code>invalid</code>.
	 */
	public String getSourceId() {
		if (!this.isValid()) {
			return null;
		}

		return this.sourceId;
	}

	/**
	 * Returns the object identifier.
	 * 
	 * @return The object identifier or <code>null</code>, if the id is <code>invalid</code>.
	 */
	public JsonArray getObjectId() {
		if (!this.isValid()) {
			return null;
		}

		return this.objectId;
	}

	/**
	 * Checks if the id is valid.
	 * 
	 * @return <code>true</code>, if the id is valid; otherwise <code>false</code>.
	 */
	public boolean isValid() {
		return this.sourceId != null && this.objectId != null;
	}

	/**
	 * Checks if the passed id has the same source information.
	 * 
	 * @param other
	 *            Another <code>GenericObjectId</code>.
	 * @return <code>true</code>, if both ids contain the same source information; otherwise <code>false</code>.
	 */
	public boolean fromSameSource(GenericObjectId other) {
		return this.sourceId.equals(other.sourceId);
	}

	@Override
	public String toString() {
		return this.getSourceId() + "." + this.getObjectId();
	}

	public int compareTo(GenericObjectId other) {
		if (this.sourceId.compareTo(other.sourceId) != 0) {
			// compares the source identifier
			return this.sourceId.compareTo(other.sourceId);
		}

		// compares the object identifier if the source identifiers are equal
		return this.objectId.compareTo(other.objectId);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.objectId == null) ? 0 : this.objectId.hashCode());
		result = prime * result + ((this.sourceId == null) ? 0 : this.sourceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (this.getClass() != obj.getClass()) {
			return false;
		}
		
		GenericObjectId other = (GenericObjectId) obj;
		if (this.objectId == null) {
			if (other.objectId != null) {
				return false;
			}
		} else if (!this.objectId.equals(other.objectId)) {
			return false;
		}
		if (this.sourceId == null) {
			if (other.sourceId != null) {
				return false;
			}
		} else if (!this.sourceId.equals(other.sourceId)) {
			return false;
		}
		return true;
	}
	
	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeRecordStart();
		
		jsonGenerator.writeRecordEntry(GenericObjectId.SOURCE_ID_ATTRIBUTE_NAME, new JsonString(this.getSourceId()));
		jsonGenerator.writeRecordEntry(GenericObjectId.OBJECT_ID_ATTRIBUTE_NAME, this.objectId);
		
		jsonGenerator.writeRecordEnd();
	}

	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		jsonParser.skipToken(); // skip '{'
		
		if (!GenericObjectId.SOURCE_ID_ATTRIBUTE_NAME.equals(jsonParser.nextFieldName())) {
			throw new JsonParseException("Source identifier is missing.", null);
		}
		
		this.sourceId = jsonParser.nextJsonString().getStringValue();
		
		if (!GenericObjectId.OBJECT_ID_ATTRIBUTE_NAME.equals(jsonParser.nextFieldName())) {
			throw new JsonParseException("Object identifier is missing.", null);
		}
		
		this.objectId = jsonParser.nextJsonArray();
		
		jsonParser.skipToken(); // skip '}'
	}
}
