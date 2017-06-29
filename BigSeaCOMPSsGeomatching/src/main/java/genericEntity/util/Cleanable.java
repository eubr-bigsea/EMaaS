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

import java.io.Closeable;

/**
 * <code>Cleanable</code> is an interface that provides methods for easily closing a bunch of {@link Closeable} or <code>Cleanable</code> instances.
 * 
 * @author Matthias Pohl
 */
public interface Cleanable {

	/**
	 * Registers a {@link Closeable} instance.
	 * 
	 * @param closeableResource
	 *            The <code>Closeable</code> resource that shall be closed during the next {@link #cleanUp()} call. This method has no effects, if
	 *            <code>null</code> was passed.
	 */
	public void registerCloseable(Closeable closeableResource);

	/**
	 * Registers a {@link Cleanable} instance.
	 * 
	 * @param cleanableResource
	 *            The <code>Cleanable</code> resource that shall be closed during the next {@link #cleanUp()} call. This method has no effects, if
	 *            <code>null</code> was passed.
	 */
	public void registerCleanable(Cleanable cleanableResource);

	/**
	 * Closes all registered {@link Closeable} and <code>Cleanable</code> instances.
	 */
	public void cleanUp();

}