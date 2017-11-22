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

package genericEntity.util.data.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import genericEntity.util.data.Jsonable;

/**
 * <code>InMemoryStorage</code> stores {@link Jsonable} instances in memory.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The <code>Jsonable</code> type whose instances are stored.
 */
public class InMemoryStorage<T extends Jsonable> extends AbstractGenericObjectStorage<T> {

	private class InMemoryReader implements JsonableReader<T> {

		public Iterator<T> iterator() {
			return InMemoryStorage.this.data.iterator();
		}
		
		public void close() throws IOException {
			// nothing to do
		}

	}

	private class InMemoryWriter extends AbstractJsonableWriter<T> {

		private InMemoryWriter() {
			InMemoryStorage.this.data.clear();
		}

		public boolean add(T value) {
			return InMemoryStorage.this.data.add(value);
		}

		public void close() throws IOException {
			// nothing to do
		}

	}

	private Collection<T> data = new ArrayList<T>();

	/**
	 * Initializes a <code>InMemoryStorage</code> instance.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs while accessing the initial content.
	 */
	public InMemoryStorage() throws IOException {
		this(null);
	}

	/**
	 * Initializes a <code>InMemoryStorage</code> instance with the passed initial content.
	 * 
	 * @param initialContent
	 *            The content that shall be added to the storage initially.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs while accessing the initial content.
	 */
	public InMemoryStorage(Collection<T> initialContent) throws IOException {
		if (initialContent != null) {
			JsonableWriter<T> initialWriter = this.getWriter();

			for (T element : initialContent) {
				initialWriter.add(element);
			}

			initialWriter.close();
		}
	}

	public JsonableReader<T> getReader() {
		return new InMemoryReader();
	}

	public JsonableWriter<T> getWriter() {
		// TODO: make this a singleton?!
		return new InMemoryWriter();
	}

	public int size() {
		return this.data.size();
	}

}
