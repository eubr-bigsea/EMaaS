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

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.util.BoundType;
import genericEntity.util.ReflectUtil;
import genericEntity.util.data.AutoJsonable;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;

/**
 * The json serialization support for classes.
 * 
 * @author Arvid.Heise
 * 
 * @param <T>
 *            the class to wrap
 */
public class CompositeJsonSerialization<T> extends AutoJsonSerialization<T> {
	
	private final Map<Field, AutoJsonSerialization> jsonableFields = new LinkedHashMap<Field, AutoJsonSerialization>();
	private final Map<Field, Primitive> jsonablePrimitives = new LinkedHashMap<Field, Primitive>();
	private AutoJsonSerialization<? super T> superTypeInfo;
	private static final Logger Log = Logger.getLogger(GenericJsonParser.class.getPackage().getName());
	private final boolean autoJsonable;

	/**
	 * Initializes the serialization with the given type.
	 * 
	 * @param type
	 *            the wrapped type
	 */
	@SuppressWarnings("unchecked")
	public CompositeJsonSerialization(BoundType type) {
		super(type);
		this.autoJsonable = AutoJsonable.class.isAssignableFrom(type.getType());
		final Class<T> actualClass = (Class<T>) type.getType();
		final Type superclass = actualClass.getGenericSuperclass();
		if (superclass != null) {
			if (superclass instanceof ParameterizedType)
				this.superTypeInfo = (AutoJsonSerialization<? super T>) JsonTypeManager.getInstance().getTypeInfo(
						ReflectUtil.resolveParamizedSuperclass(type, (ParameterizedType) superclass));
			else
				this.superTypeInfo = (AutoJsonSerialization<? super T>) JsonTypeManager.getInstance().getTypeInfo(superclass);
		}

		for (final Field field : actualClass.getDeclaredFields()) {
			if ((field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) != 0)
				continue;

			field.setAccessible(true);
			final BoundType fieldType = BoundType.of(field.getType());
			if (!this.initializeAsPrimitive(field, fieldType) && !this.initializeAsEnum(field, fieldType)
					&& !this.initializeAsArray(field, fieldType) && !this.initializeAsCollection(field, fieldType)
					&& !this.initializeAsRecord(field, fieldType) && !this.initializeAsTypeVariable(field))
				Log.warn(type + " is not fully AutoJsonable @ field " + field.getName());
			// throw new IllegalArgumentException("Type is not fully AutoJsonable @ field " + field.getName());
		}
	}

	/**
	 * Returns the complex fields of the wrapped type.
	 * 
	 * @return the complex fields
	 */
	@SuppressWarnings("unchecked")
	public Map<Field, CompositeJsonSerialization<?>> getComplexFields() {
		return (Map) this.jsonableFields;
	}

	/**
	 * Returns the primitive fields of the wrapped type.
	 * 
	 * @return the primitive fields
	 */
	@SuppressWarnings("unchecked")
	public Map<Field, Primitive<?>> getPrimitiveFields() {
		return (Map) this.jsonablePrimitives;
	}

	/**
	 * Returns the super type json serialization.
	 * 
	 * @return the super type json serialization
	 */
	public AutoJsonSerialization<? super T> getSuperTypeInfo() {
		return this.superTypeInfo;
	}

	private AutoJsonSerialization<?> getTypeInfo(final Field field) {
		Type genericType = field.getGenericType();
		if (genericType instanceof TypeVariable<?>) {
			final BoundType resolvedType = ReflectUtil.resolveType(this.getType(), (TypeVariable<?>) genericType);
			genericType = resolvedType.getParameterizedType() == null ? resolvedType.getType() : resolvedType.getParameterizedType();
		}
		return JsonTypeManager.getInstance().getTypeInfo(genericType);
	}

	private boolean initializeAsArray(Field field, BoundType type) {
		if (type.getType().isArray()) {
			this.jsonableFields.put(field, JsonTypeManager.getInstance().getTypeInfo(type));
			return true;
		}
		return false;
	}

	private boolean initializeAsCollection(Field field, BoundType type) {
		if (Collection.class.isAssignableFrom(type.getType())) {
			this.jsonableFields.put(field, JsonTypeManager.getInstance().getTypeInfo(field.getGenericType()));
			return true;
		}
		return false;
	}

	private boolean initializeAsEnum(Field field, BoundType type) {
		if (type.getType().isEnum()) {
			this.jsonableFields.put(field, JsonTypeManager.getInstance().getTypeInfo(type));
			return true;
		}
		return false;
	}

	private boolean initializeAsPrimitive(Field field, BoundType type) {
		final Primitive<?> primitive = JsonTypeManager.getInstance().getPrimitive(type.getType());
		if (primitive != null) {
			this.jsonablePrimitives.put(field, primitive);
			return true;
		}
		return false;
	}

	private boolean initializeAsRecord(Field field, BoundType type) {
		if (AutoJsonable.class.isAssignableFrom(type.getType())) {
			this.jsonableFields.put(field, JsonTypeManager.getInstance().getTypeInfo(type));
			return true;
		}
		return false;
	}

	private boolean initializeAsTypeVariable(Field field) {
		final Type genericType = field.getGenericType();
		if (genericType instanceof TypeVariable<?>) {
			final TypeVariable<?> typeVar = (TypeVariable<?>) genericType;

			final BoundType actualType = ReflectUtil.resolveType(this.getType(), typeVar);
			return this.initializeAsPrimitive(field, actualType) || this.initializeAsEnum(field, actualType)
					|| this.initializeAsArray(field, actualType) || this.initializeAsCollection(field, actualType)
					|| this.initializeAsRecord(field, actualType);
		}
		return true;
	}

	/**
	 * Creates an instance of the wrapped type and initializes the content with the json provided by the given {@link GenericJsonParser}. This method
	 * might reuse the currentValue but should in most cases use the actual type information of the currentValue to create a new instance.
	 * 
	 * @param parser
	 *            the parser to read from
	 * @param currentValue
	 *            the current value of the field which should be populated by the result of this method or null if this is the root object
	 * @return an instance of <code>T</code>
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	@Override
	public T read(GenericJsonParser<?> parser, Object currentValue) throws JsonParseException, IOException {
		final Class<T> declaredType = this.getRawType();
		if (Jsonable.class.isAssignableFrom(declaredType))
			return this.readJsonable(parser, declaredType);

		final Class<T> serializedType = this.readSerializedType(parser, false, declaredType);
		final T instance = ReflectUtil.newInstance(serializedType == null ? declaredType : serializedType);
		if (instance instanceof Jsonable)
			((Jsonable) instance).fromJson(parser);
		else if (serializedType != null)
			JsonTypeManager.getInstance().getTypeInfo(serializedType).readFields(parser, instance);
		else
			this.readFields(parser, instance);
		parser.skipToken(JsonToken.END_OBJECT);
		return instance;
	}

	private void readComplexFields(GenericJsonParser<?> parser, T instance) throws JsonParseException, IOException, EOFException, IllegalAccessException {
		for (final Entry<Field, AutoJsonSerialization> entry : this.jsonableFields.entrySet()) {
			final Field field = entry.getKey();
			parser.skipFieldName(entry.getKey().getName());

			if (parser.currentToken() == JsonToken.VALUE_NULL) {
				field.set(instance, null);
				parser.skipToken(JsonToken.VALUE_NULL);
			} else
				field.set(instance, this.getTypeInfo(field).read(parser, field.get(instance)));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see genericEntity.util.data.json.auto.JsonTypeInfo#readFields(genericEntity.util.data.json.GenericJsonParser, I)
	 */
	@Override
	public void readFields(GenericJsonParser<?> parser, T instance) throws JsonParseException, IOException, EOFException {
		try {
			this.readPrimitiveFields(parser, instance);

			this.readComplexFields(parser, instance);
		} catch (final IllegalAccessException e) {
			// would have failed earlier @ Field#setAccessible in the ctor
			throw new IllegalStateException();
		}

		if (this.superTypeInfo != null)
			this.superTypeInfo.readFields(parser, instance);
	}

	private T readJsonable(GenericJsonParser<?> parser, final Class<T> declaredType) throws JsonParseException, IOException {
		final Class<T> serializedType = this.readSerializedType(parser, true, declaredType);
		final T instance = ReflectUtil.newInstance(serializedType == null ? declaredType : serializedType);
		((Jsonable) instance).fromJson(parser);
		finishTypeSerializationRead(parser, serializedType);
		return instance;
	}

	@SuppressWarnings("unchecked")
	private void readPrimitiveFields(GenericJsonParser<?> parser, T instance) throws JsonParseException, IOException, IllegalAccessException {
		for (final Entry<Field, Primitive> entry : this.jsonablePrimitives.entrySet()) {
			parser.skipFieldName(entry.getKey().getName());
			entry.getKey().set(instance, entry.getValue().read(parser));
		}
	}

	/**
	 * Writes the given instance to the specified {@link GenericJsonGenerator}.
	 * 
	 * @param generator
	 *            the generator to write to
	 * @param jsonable
	 *            the jsonable to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	@Override
	public void write(GenericJsonGenerator generator, T jsonable) throws JsonGenerationException, IOException {
		if (!this.autoJsonable && jsonable instanceof AutoJsonable)
			throw new IllegalArgumentException("Cannot serialize " + jsonable + " since it is not " + AutoJsonable.class.getSimpleName());

		if (jsonable instanceof Jsonable)
			((Jsonable) jsonable).toJson(generator);
		else {
			generator.writeRecordStart();
			this.writeFields(generator, jsonable);
			generator.writeRecordEnd();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see genericEntity.util.data.json.auto.JsonTypeInfo#writeFields(genericEntity.util.data.json.GenericJsonGenerator, java.lang.Object)
	 */
	@Override
	public void writeFields(GenericJsonGenerator generator, Object jsonable) throws JsonGenerationException, IOException {
		try {
			writePrimitiveFields(generator, jsonable);
			writeComplexFields(generator, jsonable);
		} catch (final IllegalAccessException e) {
			// would have failed earlier @ Field#setAccessible in the ctor
		}
		if (this.superTypeInfo != null)
			this.superTypeInfo.writeFields(generator, jsonable);
	}

	@SuppressWarnings("unchecked")
	private void writeComplexFields(GenericJsonGenerator generator, Object jsonable) throws JsonGenerationException, IOException, IllegalAccessException {
		for (final Entry<Field, AutoJsonSerialization> entry : this.jsonableFields.entrySet()) {
			final Field field = entry.getKey();
			generator.writeRecordFieldName(field.getName());
			final Object value = field.get(jsonable);
			if (value == null)
				generator.writeJsonNull();
			else if (ReflectUtil.isSameTypeOrPrimitive(field.getType(), value.getClass()))
				entry.getValue().write(generator, value);
			else
				((AutoJsonSerialization) JsonTypeManager.getInstance().getTypeInfo(value.getClass(), field.getGenericType())).writeWithType(
						generator, value);
		}
	}

	@SuppressWarnings("unchecked")
	private void writePrimitiveFields(GenericJsonGenerator generator, Object jsonable) throws JsonGenerationException, IOException,
			IllegalAccessException {
		for (final Entry<Field, Primitive> entry : this.jsonablePrimitives.entrySet()) {
			generator.writeRecordFieldName(entry.getKey().getName());
			entry.getValue().write(entry.getKey().get(jsonable), generator);
		}
	}
}