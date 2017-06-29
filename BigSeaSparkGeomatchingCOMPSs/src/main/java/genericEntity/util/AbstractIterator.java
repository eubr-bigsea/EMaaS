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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <code>AbstractIterator</code> is an <code>abstract</code> class that should be used within all iterator-like
 * classes. It implements most of the iterator functionality.
 * 
 * @author Matthias Pohl
 * 
 * @param <ElementType>
 *            The type of the elements over which the iterator iterates.
 */
public abstract class AbstractIterator<ElementType> implements Iterator<ElementType> {

	private ElementType nextElement;

	private boolean firstElementWasLoaded = false;

	public final boolean hasNext() {
		this.loadFirstElement(); // loads the first element if needed

		return this.nextElement != null;
	}

	public final ElementType next() {
		this.loadFirstElement(); // loads the first element if needed

		ElementType currentElement = this.getNextElement();
		this.setNextElement();

		// java specification - NoSuchElementException should be thrown if the end of the underlying data was reached
		if (currentElement == null) {
			throw new NoSuchElementException("End of data reached...");
		}
		
		return currentElement;
	}

	/**
	 * This method is not implemented and will throw an {@link UnsupportedOperationException}.
	 * 
	 * @see Iterator#remove()
	 */
	public final void remove() {
		throw new UnsupportedOperationException("AbstractIterator does not implement remove().");
	}

	/**
	 * Checks whether the first element was already pre-loaded.
	 * 
	 * @return <code>true</code>, if it was already pre-loaded; otherwise <code>false</code>.
	 */
	private boolean firstElementWasLoaded() {
		return this.firstElementWasLoaded;
	}

	/**
	 * Checks whether the first element was pre-loaded. If it is not pre-loaded the pre-loading will be done.
	 */
	private void loadFirstElement() {
		// load first element if it is not already done
		// loading the first element can't be done within the constructor!
		if (!this.firstElementWasLoaded()) {
			this.nextElement = this.loadNextElement();

			// disables pre-loading for the next call, if a first element was already loaded
			if (this.nextElement != null) {
				this.firstElementWasLoaded = true;
			}
		}
	}

	/**
	 * Returns the instance stored in {@link #nextElement}.
	 * 
	 * @return The instance stored in the <code>nextElement</code> member.
	 */
	private ElementType getNextElement() {
		return this.nextElement;
	}

	/**
	 * Sets the {@link #nextElement} member by calling {@link #loadNextElement()}.
	 */
	private void setNextElement() {
		this.nextElement = this.loadNextElement();
	}

	/**
	 * Returns the element of the next iteration step. This method needs to be implemented by each sub-class.
	 * 
	 * @return The next element.
	 */
	protected abstract ElementType loadNextElement();

}
