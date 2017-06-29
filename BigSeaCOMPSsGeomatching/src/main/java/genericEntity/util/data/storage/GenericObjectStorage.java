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

import genericEntity.util.data.Jsonable;

/**
 * <code>GenericObjectStorage</code> stores {@link Jsonable} instances. These instances can be accessed using a {@link JsonableReader} and added using a
 * {@link JsonableWriter}.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The {@link Jsonable} type whose instances are stored in the <code>GenericObjectStorage</code>.
 *            
 * @see AbstractGenericObjectStorage
 */
public interface GenericObjectStorage<T extends Jsonable> extends JsonReadable<T>, JsonWritable<T> {

	// no additional interface methods

}
