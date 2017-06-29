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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.util.BoundType;
import genericEntity.util.ReflectUtil;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;

/**
 * Collection json serialization support.
 * 
 * @author Arvid.Heise
 * 
 * @param <E>
 *            the element type
 * @param <C>
 *            the collection type
 */
class CollectionJsonSerialization<E, C extends Collection<E>> extends AutoJsonSerialization<C> {
	private final AutoJsonSerialization<E> innerTypeInfo;
	private final Primitive<E> innerPrimitive;
	private final BoundType innerType;

	@SuppressWarnings("serial")
	private final static Map<Class<?>, Class<?>> DEFAULT_INSTANCES = new IdentityHashMap<Class<?>, Class<?>>() {
		{
			this.put(List.class, ArrayList.class);
			this.put(Set.class, HashSet.class);
			this.put(Collection.class, ArrayList.class);
			this.put(Iterable.class, ArrayList.class);
		}
	};

	/**
	 * Initializes the json serialization for the given type.
	 * 
	 * @param genericType
	 *            the type to wrap
	 */
	@SuppressWarnings("unchecked")
	public CollectionJsonSerialization(BoundType genericType) {
		super(genericType);
		this.innerType = genericType.getParameters()[0];
		this.innerPrimitive = (Primitive<E>) JsonTypeManager.getInstance().getPrimitive(this.innerType.getType());
		this.innerTypeInfo = (AutoJsonSerialization<E>) (this.innerPrimitive == null ? JsonTypeManager.getInstance().getTypeInfo(this.innerType)
				: null);
	}

	private Class<?> findActualType(Class<?> type, Object currentValue) {
		if (this.isInstantiable(type))
			return type;
		if (currentValue != null && this.isInstantiable(currentValue.getClass()))
			return currentValue.getClass();
		Class<?> superType = type;
		do
			if (DEFAULT_INSTANCES.containsKey(superType))
				return DEFAULT_INSTANCES.get(superType);
		while ((superType = superType.getSuperclass()) != null);
		throw new IllegalArgumentException(type + " is not instantiable");
	}

	@SuppressWarnings("unchecked")
	@Override
	public C read(GenericJsonParser<?> parser, Object currentValue) throws JsonParseException, IOException {
		Class<C> actualType;
		boolean augmented = false;
		if (parser.currentToken() == JsonToken.START_OBJECT) {
			parser.skipToken(JsonToken.START_OBJECT);
			parser.skipFieldName("class");
			// augmented type
			final String className = parser.nextString();
			try {
				actualType = (Class<C>) Class.forName(className);
			} catch (final ClassNotFoundException e) {
				throw new JsonParseException("Could not resolve class " + className, null);
			}
			augmented = true;
			parser.skipFieldName("elements");
		} else
			actualType = (Class<C>) this.findActualType(this.getRawType(), currentValue);
		parser.skipToken(JsonToken.START_ARRAY);

		final C instance = ReflectUtil.newInstance(actualType);

		while (parser.currentToken() != JsonToken.END_ARRAY)
			if (parser.currentToken() == JsonToken.VALUE_NULL) {
				instance.add(null);
				parser.skipToken(JsonToken.VALUE_NULL);
			} else if (this.innerPrimitive != null)
				instance.add(this.innerPrimitive.read(parser));
			else
				instance.add(this.innerTypeInfo.read(parser));
		parser.skipToken(JsonToken.END_ARRAY);

		if (augmented)
			parser.skipToken(JsonToken.END_OBJECT);

		return instance;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(GenericJsonGenerator generator, C collection) throws JsonGenerationException, IOException {
		generator.writeArrayStart();

		for (final E value : collection) {
			if (value == null) {
				generator.writeJsonNull();
				continue;
			}

			if (this.innerPrimitive != null)
				this.innerPrimitive.write(value, generator);
			else if (!ReflectUtil.isSameTypeOrPrimitive(this.innerType.getType(), value.getClass()))
				((CompositeJsonSerialization) JsonTypeManager.getInstance().getTypeInfo(value.getClass())).writeWithType(generator, value);
			else
				this.innerTypeInfo.write(generator, value);
		}

		generator.writeArrayEnd();
	}

	@Override
	protected void writeWithType(GenericJsonGenerator generator, C value) throws JsonGenerationException, IOException {
		if (DEFAULT_INSTANCES.containsValue(value.getClass()) || !this.isInstantiable(value.getClass()))
			this.write(generator, value);
		else {
			generator.writeRecordStart();
			generator.writeStringRecordEntry("class", value.getClass().getName());
			generator.writeRecordFieldName("elements");
			this.write(generator, value);
			generator.writeRecordEnd();
		}
	}
}