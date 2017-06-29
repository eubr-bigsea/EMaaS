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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Logically concatenates several {@link Iterator}s to one big iterator.
 * 
 * @author Arvid.Heise
 * 
 * @param <T>
 *            the type of the {@link Iterator}
 */
public class ChainedIterator<T> extends AbstractIterator<T> {
	/**
	 * Creates a chained iterator from {@link Iterable}s.
	 * 
	 * @param <T>
	 *            the type of the {@link Iterable}s
	 * @param iterables
	 *            the iterables from which to create a {@link ChainedIterator}.
	 * @return a {@link ChainedIterator} iterating over all iterables
	 */
	public static <T> ChainedIterator<T> fromIterables(Iterable<Iterable<T>> iterables) {
		final List<Iterator<T>> list = new ArrayList<Iterator<T>>();
		for (final Iterable<T> iterable : iterables)
			list.add(iterable.iterator());
		return new ChainedIterator<T>(list);
	}

	private final Iterator<Iterator<T>> iterators;

	private Iterator<T> currentIterator;

	/**
	 * Initializes the chained iterator from multiple single iterators.
	 * 
	 * @param iterators
	 *            the iterators to chain
	 */
	public ChainedIterator(Iterable<Iterator<T>> iterators) {
		this.iterators = iterators.iterator();
	}

	/**
	 * Initializes the chained iterator from multiple single iterators.
	 * 
	 * @param iterators
	 *            the iterators to chain
	 */
	public ChainedIterator(Iterator<Iterator<T>> iterators) {
		this.iterators = iterators;
	}

	/**
	 * Initializes the chained iterator from multiple single iterators.
	 * 
	 * @param iterators
	 *            the iterators to chain
	 */
	public ChainedIterator(Iterator<T>... iterators) {
		this.iterators = Arrays.asList(iterators).iterator();
	}

	@Override
	protected T loadNextElement() {
		while (this.currentIterator == null || !this.currentIterator.hasNext()) {
			if (!this.iterators.hasNext())
				return null;
			this.currentIterator = this.iterators.next();
		}
		return this.currentIterator.next();
	}

}
