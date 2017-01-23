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
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;

import genericEntity.util.GlobalConfig;
import genericEntity.util.data.Jsonable;

/**
 * <code>AbstractGenericObjectStorage</code> stores {@link Jsonable} instances. It is a skeleton class of the {@link GenericObjectStorage} and implements some of the
 * methods that are provided by this interface.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The {@link Jsonable} type whose instances are stored in the <code>GenericObjectStorage</code>.
 */
public abstract class AbstractGenericObjectStorage<T extends Jsonable> implements GenericObjectStorage<T> {

	/**
	 * <code>AbstractGenericObjectStorage.AbstractJsonableWriter</code> implements some functionality that shall be provided by all {@link JsonableWriter}
	 * sub-classes.
	 * 
	 * @author Matthias Pohl
	 * 
	 * @param <T>
	 *            The {@link Jsonable} type whose instances are stored in the <code>GenericObjectStorage</code>.
	 */
	protected abstract static class AbstractJsonableWriter<T extends Jsonable> implements JsonableWriter<T> {

		@Override
		public boolean addAll(Collection<T> collection) throws JsonGenerationException, IOException {
			boolean contentChanged = false;

			for (T value : collection) {
				if (this.add(value)) {
					contentChanged = true;
				}
			}

			return contentChanged;
		}

	}

	private boolean formattedJsonEnabled = GlobalConfig.getInstance().formattedJsonIsEnabled();

	@Override
	public boolean isFormattedJson() {
		return this.formattedJsonEnabled;
	}

	@Override
	public void enableFormattedJson() {
		this.formattedJsonEnabled = true;
	}

	@Override
	public void disableFormattedJson() {
		this.formattedJsonEnabled = false;
	}
}
