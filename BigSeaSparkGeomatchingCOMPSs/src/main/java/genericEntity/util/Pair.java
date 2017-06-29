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

import genericEntity.util.data.AutoJsonable;

/**
 * A <code>Pair</code> is a container that stores two objects.
 * 
 * @author Matthias Pohl
 * 
 * @param <T1>
 *            The type of the first element.
 * @param <T2>
 *            The type of the second element.
 */
public class Pair<T1, T2> implements AutoJsonable {

	private T1 firstElement;
	private T2 secondElement;

	/**
	 * Initializes an empty pair.
	 */
	public Pair() {
		// TODO: use dummy values and package-default constructor
		this.setFirstElement(null);
		this.setSecondElement(null);
	}
	
	/**
	 * Generates a pair of the given two elements. The types of both elements are specified by the generic parameters <code>T1</code> and
	 * <code>T2</code>.
	 * 
	 * @param first
	 *            The first element.
	 * @param second
	 *            The second element.
	 * @throws NullPointerException
	 *             If at least one of the parameters is <code>null</code>.
	 */
	public Pair(T1 first, T2 second) {
		this.firstElement = first;
		this.secondElement = second;
	}

	/**
	 * Returns the first element.
	 * 
	 * @return The first element.
	 */
	public T1 getFirstElement() {
		return this.firstElement;
	}

	/**
	 * Sets the first element.
	 * 
	 * @param elem
	 *            The new first element.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed.
	 */
	public void setFirstElement(T1 elem) {
		this.firstElement = elem;
	}

	/**
	 * Returns the second element.
	 * 
	 * @return The second element.
	 */
	public T2 getSecondElement() {
		return this.secondElement;
	}
	
	/**
	 * Sets the second element.
	 * 
	 * @param elem
	 *            The new second element.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed.
	 */
	public void setSecondElement(T2 elem) {
		this.secondElement = elem;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof Pair<?, ?>)) {
			return false;
		}

		Pair<?, ?> other = (Pair<?, ?>) obj;

		// first element
		if (this.firstElement == null) {
			if (other.firstElement != null) {
				return false;
			}
		} else if (!this.firstElement.equals(other.firstElement)) {
			return false;
		}

		// second element
		if (this.secondElement == null) {
			if (other.secondElement != null) {
				return false;
			}
		} else if (!this.secondElement.equals(other.secondElement)) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether the passed Pair is the symmetric correspondent of this instance.
	 * 
	 * @param otherPair
	 *            A <code>Pair</code> with which the symmetric relation shall be checked.
	 * @return <code>true</code>, if the passed <code>Pair</code> object is the symmetric correspondent of this instance; otherwise <code>false</code>
	 */
	public boolean isSymmetricTo(Pair<T1, T2> otherPair) {
		return this.firstElement.equals(otherPair.getSecondElement()) && this.secondElement.equals(otherPair.getFirstElement());
	}

	@Override
	public String toString() {
		return "[" + (this.firstElement != null ? this.firstElement.toString() : "null") + "; "
				+ (this.secondElement != null ? this.secondElement.toString() : "null") + "]";
	}

	/**
	 * Checks whether this instance is reflexive.
	 * 
	 * @return <code>true</code>, if this instance is reflexive; otherwise <code>false</code>.
	 */
	public boolean isReflexive() {
		return this.firstElement.equals(this.secondElement);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.firstElement == null) ? 0 : this.firstElement.hashCode());
		result = prime * result + ((this.secondElement == null) ? 0 : this.secondElement.hashCode());
		return result;
	}
}
