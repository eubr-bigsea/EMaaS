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

package genericEntity.util;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

/**
 * Represents an rectified, generic type, which is only equal to another {@link BoundType} iff all bounds are exactly the same.
 * 
 * @author Arvid.Heise
 */
public class BoundType {
	/**
	 * Wraps an array of {@link Class} to an array of {@link BoundType}s without parameters.
	 * 
	 * @param rawTypes
	 *            the classes to wrap
	 * @return an array containing a {@link BoundType} for each raw type
	 */
	public static BoundType[] arrayOf(Class<?>... rawTypes) {
		final BoundType[] types = new BoundType[rawTypes.length];
		for (int index = 0; index < types.length; index++)
			types[index] = BoundType.of(rawTypes[index]);
		return types;
	}

	/**
	 * Creates a {@link BoundType} around the given raw type with additional type parameters.
	 * 
	 * @param rawType
	 *            the class to wrap
	 * @param parameters
	 *            the type parameters
	 * @return a {@link BoundType} representing the raw type and its parameters
	 */
	public static BoundType of(Class<?> rawType, BoundType... parameters) {
		final BoundType boundedType = new BoundType(rawType);
		boundedType.parameters = parameters;
		return boundedType;
	}

	/**
	 * Creates a {@link BoundType} around the given raw type with additional type parameters.
	 * 
	 * @param rawType
	 *            the classes to wrap
	 * @param subType1
	 *            the first type parameter
	 * @param otherTypes
	 *            additional type parameters
	 * @return a {@link BoundType} representing the raw type and its parameters
	 */
	public static BoundType of(Class<?> rawType, Class<?> subType1, Class<?>... otherTypes) {
		final BoundType boundedType = new BoundType(rawType);
		boundedType.parameters = new BoundType[1 + otherTypes.length];
		boundedType.parameters[0] = BoundType.of(subType1);
		for (int index = 1; index < boundedType.parameters.length; index++)
			boundedType.parameters[index] = BoundType.of(otherTypes[index - 1]);
		return boundedType;
	}

	/**
	 * Creates a {@link BoundType} for the given {@link ParameterizedType}.
	 * 
	 * @param type
	 *            the type to wrap
	 * @return a recursively resolved type
	 */
	public static BoundType of(ParameterizedType type) {
		return new BoundType(type);
	}

	private BoundType[] parameters;

	private final Class<?> rawType;

	private ParameterizedType type;

	/**
	 * Initializes a {@link BoundType} around the given raw type.
	 * 
	 * @param type
	 *            the type to wrap
	 */
	public BoundType(Class<?> type) {
		this.rawType = type;
		this.parameters = new BoundType[0];
	}

	/**
	 * Initializes a {@link BoundType} around the given parameterized type. Recursively determines the static bounds of all superclasses.
	 * 
	 * @param type
	 *            the type to wrap
	 */
	public BoundType(ParameterizedType type) {
		this.type = type;
		this.rawType = (Class<?>) type.getRawType();
		this.parameters = ReflectUtil.getStaticBoundTypes(type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final BoundType other = (BoundType) obj;
		if (this.rawType == null) {
			if (other.rawType != null)
				return false;
		} else if (!this.rawType.equals(other.rawType))
			return false;
		if (!Arrays.equals(this.parameters, other.parameters))
			return false;
		return true;
	}

	/**
	 * Returns the wrapped parameterized type or null if this {@link BoundType} was not created around a {@link ParameterizedType}.
	 * 
	 * @return the wrapped parameterized type or null
	 */
	public ParameterizedType getParameterizedType() {
		return this.type;
	}

	/**
	 * Returns the bound types or an empty array if none exists.
	 * 
	 * @return the bound types
	 */
	public BoundType[] getParameters() {
		return this.parameters;
	}

	/**
	 * Returns the raw type.
	 * 
	 * @return the raw type
	 */
	public Class<?> getType() {
		return this.rawType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.rawType == null ? 0 : this.rawType.hashCode());
		result = prime * result + Arrays.hashCode(this.parameters);
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(this.rawType.getSimpleName());
		if (this.parameters.length > 0) {
			builder.append('<');
			builder.append(this.parameters[0]);
			for (int index = 1; index < this.parameters.length; index++) {
				builder.append(", ");
				builder.append(this.parameters[index]);
			}
			builder.append('>');
		}
		return builder.toString();
	}

}