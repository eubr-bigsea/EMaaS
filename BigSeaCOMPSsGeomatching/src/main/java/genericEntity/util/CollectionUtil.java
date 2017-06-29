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

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Convenience methods for collections.
 * 
 * @author Arvid.Heise
 */
public class CollectionUtil {
	/**
	 * Returns the last element of the collection.
	 * 
	 * @param <T>
	 *            the element type
	 * @param list
	 *            the collection
	 * @return the last element or null if the list is empty
	 */
	@SuppressWarnings("unchecked")
	public static <T> T last(List<T> list) {
		if (list instanceof Deque<?>)
			return ((Deque<T>) list).getLast();
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * Returns the last element of the collection.
	 * 
	 * @param <T>
	 *            the element type
	 * @param iterable
	 *            the collection
	 * @return the last element or null if the list is empty
	 */
	public static <T> T last(Iterable<T> iterable) {
		if (iterable instanceof Deque<?>)
			return ((Deque<T>) iterable).getLast();
		if (iterable instanceof List<?>) {
			List<T> list = (List<T>) iterable;
			return list.isEmpty() ? null : list.get(list.size() - 1);
		}
		Iterator<T> iterator = iterable.iterator();
		T last = null;
		while (iterator.hasNext())
			last = iterator.next();
		return last;
	}

	/**
	 * Returns the first element of the collection.
	 * 
	 * @param <T>
	 *            the element type
	 * @param list
	 *            the collection
	 * @return the first element or null if the list is empty
	 */
	@SuppressWarnings("unchecked")
	public static <T> T first(List<T> list) {
		if (list instanceof Deque<?>)
			return ((Deque<T>) list).getFirst();
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Returns the first element of the collection.
	 * 
	 * @param <T>
	 *            the element type
	 * @param iterable
	 *            the collection
	 * @return the first element or null if the list is empty
	 */
	public static <T> T first(Iterable<T> iterable) {
		if (iterable instanceof Deque<?>)
			return ((Deque<T>) iterable).getFirst();
		if (iterable instanceof List<?>) {
			List<T> list = (List<T>) iterable;
			return list.isEmpty() ? null : list.get(0);
		}
		Iterator<T> iterator = iterable.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	/**
	 * Materializes the elements of the given {@link Iterator} into a {@link List}.
	 * 
	 * @param <T>
	 *            the element type
	 * @param iterator
	 *            the {@link Iterator} to materialize
	 * @return the {@link List} containing the materialized elements.
	 */
	public static <T> List<T> asList(Iterator<T> iterator) {
		ArrayList<T> list = new ArrayList<T>();
		while (iterator.hasNext())
			list.add(iterator.next());
		return list;
	}
}
