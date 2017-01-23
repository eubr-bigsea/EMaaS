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

/**
 * 
 */
package genericEntity.util.data.json.auto;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.util.BoundType;
import genericEntity.util.ReflectUtil;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;

/**
 * Json serialization for arrays.
 * 
 * @author Arvid.Heise
 * 
 * @param <E>
 *            the element type of the array
 */
// type parameter has to be Object to since int[] cannot be casted to Object[] for the return value of read
class ArrayJsonSerialization<E> extends AutoJsonSerialization<Object> {
	private final AutoJsonSerialization<E> innerTypeInfo;
	private final Primitive<E> innerPrimitive;
	private final Class<E> innerType;

	/**
	 * Initializes the serialization for the given array type.
	 * 
	 * @param type
	 *            the wrapped type of the array
	 */
	@SuppressWarnings("unchecked")
	ArrayJsonSerialization(BoundType type) {
		super(type);
		this.innerType = (Class<E>) this.getRawType().getComponentType();
		this.innerPrimitive = JsonTypeManager.getInstance().getPrimitive(this.innerType);
		this.innerTypeInfo = this.innerPrimitive == null ? JsonTypeManager.getInstance().getTypeInfo(this.innerType) : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object read(GenericJsonParser<?> parser, Object currentValue) throws JsonParseException, IOException {
		final ArrayList<E> list = new ArrayList<E>();
		parser.skipToken(JsonToken.START_ARRAY);

		while (parser.currentToken() != JsonToken.END_ARRAY)
			if (parser.currentToken() == JsonToken.VALUE_NULL) {
				list.add(null);
				parser.skipToken(JsonToken.VALUE_NULL);
			} else if (this.innerPrimitive != null)
				list.add(this.innerPrimitive.read(parser));
			else
				list.add(this.innerTypeInfo.read(parser));
		parser.skipToken(JsonToken.END_ARRAY);

		final Object array = Array.newInstance(this.innerType, list.size());
		if (!this.innerType.isPrimitive())
			return list.toArray((E[]) array);

		for (int index = 0, length = Array.getLength(array); index < length; index++)
			Array.set(array, index, list.get(index));
		return array;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(GenericJsonGenerator generator, Object jsonable) throws JsonGenerationException, IOException {

		final int length = Array.getLength(jsonable);
		generator.writeArrayStart();

		for (int index = 0; index < length; index++) {
			final Object value = Array.get(jsonable, index);
			if (value == null) {
				generator.writeJsonNull();
				continue;
			}

			if (this.innerPrimitive != null)
				this.innerPrimitive.write((E) value, generator);
			else if (!ReflectUtil.isSameTypeOrPrimitive(this.innerType, value.getClass()))
				JsonTypeManager.getInstance().getTypeInfo((Class<Object>) value.getClass()).writeWithType(generator, value);
			else
				this.innerTypeInfo.write(generator, (E) value);
		}

		generator.writeArrayEnd();
	}

}