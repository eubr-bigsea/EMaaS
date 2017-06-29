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

package genericEntity.util.data.json.auto;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.util.BoundType;
import genericEntity.util.data.AutoJsonable;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;

/**
 * Singleton for managing the {@link AutoJsonSerialization}s.
 * 
 * @author Arvid.Heise
 */
public class JsonTypeManager {
	private static JsonTypeManager INSTANCE = new JsonTypeManager();

	/**
	 * Returns the singleton instance
	 * 
	 * @return singleton instance
	 */
	public static JsonTypeManager getInstance() {
		return INSTANCE;
	}

	private final Map<BoundType, AutoJsonSerialization> typeInfo = new HashMap<BoundType, AutoJsonSerialization>();

	private final Map<Class<?>, Primitive<?>> primitives = new HashMap<Class<?>, Primitive<?>>();

	private JsonTypeManager() {
		// private for singleton
		this.initPrimitives();
	}

	/**
	 * Adds a primitive for type T. Subclasses of this type are not processed by this primitive. They must be associated manually with the primitive.
	 * 
	 * @param <T>
	 *            the basic type of the primitive
	 * @param klass
	 *            the actual type of the primitive
	 * @param primitive
	 *            the primitive which offers the json serialization for the given type
	 */
	public <T> void addPrimitive(Class<? extends T> klass, Primitive<T> primitive) {
		this.primitives.put(klass, primitive);
	}

	/**
	 * Returns the primitive for type T.
	 * 
	 * @param <T>
	 *            the type whose primitive is requested
	 * @param type
	 *            the type whose primitive is requested
	 * @return the primitive or null if none has been registered with {@link #addPrimitive(Class, Primitive)}
	 */
	@SuppressWarnings("unchecked")
	public <T> Primitive<T> getPrimitive(Class<T> type) {
		return (Primitive<T>) this.primitives.get(type);
	}

	/**
	 * Returns the {@link AutoJsonSerialization} for the given {@link BoundType}.
	 * 
	 * @param lookupType
	 *            the bound type
	 * @return the {@link AutoJsonSerialization} for T
	 * @throws NullPointerException
	 *             if lookupType is null
	 */
	public AutoJsonSerialization<?> getTypeInfo(BoundType lookupType) {

		synchronized (this) {
			final Class<?> actualType = lookupType.getType();

			if (this.typeInfo.containsKey(lookupType))
				return this.typeInfo.get(lookupType);

			if (AutoJsonable.class.isAssignableFrom(actualType)) {
				final AutoJsonSerialization value = new CompositeJsonSerialization(lookupType);
				this.typeInfo.put(lookupType, value);
				return value;
			}

			if (actualType.isArray()) {
				final AutoJsonSerialization value = new ArrayJsonSerialization(lookupType);
				this.typeInfo.put(lookupType, value);
				return value;
			}

			if (actualType.isEnum()) {
				final AutoJsonSerialization value = new EnumJsonSerialization(lookupType);
				this.typeInfo.put(lookupType, value);
				return value;
			}

			if (Collection.class.isAssignableFrom(actualType)) {
				BoundType collectionType = lookupType;
				if (lookupType.getParameters().length == 0) {
					collectionType = BoundType.of(lookupType.getType(), Object.class);

					if (this.typeInfo.containsKey(collectionType))
						return this.typeInfo.get(collectionType);
				}

				final AutoJsonSerialization value = new CollectionJsonSerialization(collectionType);
				this.typeInfo.put(collectionType, value);
				return value;
			}

			final AutoJsonSerialization value = new CompositeJsonSerialization(lookupType);
			this.typeInfo.put(lookupType, value);
			return value;
		}
	}

	/**
	 * Returns the {@link AutoJsonSerialization} for the given class.
	 * 
	 * @param <T>
	 *            the type whose {@link AutoJsonSerialization} should be returned
	 * @param actualType
	 *            the actual type
	 * @return the {@link AutoJsonSerialization} for T
	 * @throws NullPointerException
	 *             if both actualType is null
	 */
	public <T> AutoJsonSerialization<T> getTypeInfo(Class<T> actualType) {
		return this.getTypeInfo(actualType, null);
	}

	/**
	 * Returns the {@link AutoJsonSerialization} for the given class and the type parameters of the declarations if existent.
	 * 
	 * @param <T>
	 *            the type whose {@link AutoJsonSerialization} should be returned
	 * @param actualType
	 *            the actual type or null
	 * @param declaredType
	 *            the declared type or null
	 * @return the {@link AutoJsonSerialization} for T
	 * @throws NullPointerException
	 *             if both actualType and declaredType are null
	 */
	@SuppressWarnings("unchecked")
	public <T> AutoJsonSerialization<T> getTypeInfo(Class<T> actualType, Type declaredType) {
		BoundType lookupType;
		if (actualType == null)
			lookupType = BoundType.of((ParameterizedType) declaredType);
		else if (declaredType instanceof ParameterizedType)
			lookupType = BoundType.of(actualType, BoundType.of((ParameterizedType) declaredType).getParameters());
		else
			lookupType = BoundType.of(actualType);

		return (AutoJsonSerialization<T>) this.getTypeInfo(lookupType);
	}

	/**
	 * Returns the {@link AutoJsonSerialization} for the given type.
	 * 
	 * @param parameterizedType
	 *            the type whose {@link AutoJsonSerialization} should be returned
	 * @return the {@link AutoJsonSerialization} for parameterizedType
	 * @throws NullPointerException
	 *             if parameterizedType is null
	 */
	public AutoJsonSerialization<?> getTypeInfo(ParameterizedType parameterizedType) {
		return this.getTypeInfo(null, parameterizedType);
	}

	/**
	 * Returns the {@link AutoJsonSerialization} for the given type.
	 * 
	 * @param type
	 *            the type whose {@link AutoJsonSerialization} should be returned
	 * @return the {@link AutoJsonSerialization} for T
	 * @throws IllegalArgumentException
	 *             if the type is not supported (currently only {@link Class} and {@link ParameterizedType} are supported)
	 */
	public AutoJsonSerialization<?> getTypeInfo(Type type) {
		if (type instanceof ParameterizedType)
			return this.getTypeInfo((ParameterizedType) type);
		if (type instanceof Class<?>)
			return this.getTypeInfo((Class<?>) type);
		throw new IllegalArgumentException("the given type is not supported " + type);
	}

	private void initPrimitives() {
		final Primitive<Number> numberPrimitive = new Primitive<Number>(Number.class) {
			@Override
			Number read(GenericJsonParser<?> parser) throws JsonParseException, IOException {
				if (parser.currentToken() == JsonToken.VALUE_NULL) {
					parser.nextJsonNull();
					return null;
				}
				return parser.nextJsonNumber().getValue();
			}

			@Override
			public void write(Number object, GenericJsonGenerator generator) throws JsonGenerationException, IOException {
				if (object == null)
					generator.writeJsonNull();
				else
					generator.writeNumber(object);
			}
		};
		for (final Class<? extends Number> type : new Class[] { Number.class, Integer.class, Integer.TYPE, Double.class, Double.TYPE,
				BigDecimal.class, BigInteger.class })
			this.addPrimitive(type, numberPrimitive);

		final Primitive<Float> floatPrimitive = new Primitive<Float>(Float.class) {
			@Override
			Float read(GenericJsonParser<?> parser) throws JsonParseException, IOException {
				if (parser.currentToken() == JsonToken.VALUE_NULL) {
					parser.nextJsonNull();
					return null;
				}
				return parser.nextJsonNumber().getValue().floatValue();
			}

			@Override
			void write(Float object, GenericJsonGenerator generator) throws JsonGenerationException, IOException {
				if (object == null)
					generator.writeJsonNull();
				else
					generator.writeNumber(object);
			}
		};
		for (final Class<? extends Float> type : new Class[] { Float.class, Float.TYPE })
			this.addPrimitive(type, floatPrimitive);

		this.addPrimitive(String.class, new Primitive<String>(String.class) {
			@Override
			String read(GenericJsonParser<?> parser) throws JsonParseException, IOException {
				return parser.nextString();
			}

			@Override
			public void write(String object, GenericJsonGenerator generator) throws JsonGenerationException, IOException {
				generator.writeString(object);
			}
		});

		this.addPrimitive(Locale.class, new Primitive<Locale>(Locale.class) {
			@Override
			Locale read(GenericJsonParser<?> parser) throws JsonParseException, IOException {
				return new Locale(parser.nextString());
			}

			@Override
			void write(Locale object, GenericJsonGenerator generator) throws JsonGenerationException, IOException {
				generator.writeString(object.toString());
			}
		});

		final Primitive<Boolean> booleanPrimitive = new Primitive<Boolean>(Boolean.class) {
			@Override
			Boolean read(GenericJsonParser<?> parser) throws JsonParseException, IOException {
				return parser.nextJsonBoolean().getValue();
			}

			@Override
			public void write(Boolean object, GenericJsonGenerator generator) throws JsonGenerationException, IOException {
				generator.writeBoolean(object);
			}
		};
		for (final Class<? extends Boolean> type : new Class[] { Boolean.class, Boolean.TYPE })
			this.addPrimitive(type, booleanPrimitive);
		

		final Primitive<Character> charPrimitive = new Primitive<Character>(Character.class) {
			@Override
			Character read(GenericJsonParser<?> parser) throws JsonParseException, IOException {
				return parser.nextString().charAt(0);
			}

			@Override
			public void write(Character object, GenericJsonGenerator generator) throws JsonGenerationException, IOException {
				generator.writeString(object.toString());
			}
		};
		for (final Class<? extends Character> type : new Class[] { Character.class, Character.TYPE })
			this.addPrimitive(type, charPrimitive);
	}
}
