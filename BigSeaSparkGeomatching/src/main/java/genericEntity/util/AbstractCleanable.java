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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

/**
 * <code>AbstractCleanable</code> is implemented by classes that collect {@link Closeable} instances that shall be closed at the end of a process.
 * 
 * @author Matthias Pohl
 */
public abstract class AbstractCleanable implements Cleanable {

	private static final Logger logger = Logger.getLogger(AbstractCleanable.class.getPackage().getName());

	private transient Collection<Closeable> closeableResources = new ArrayList<Closeable>();
	private transient Collection<Cleanable> cleanableResources = new ArrayList<Cleanable>();

	public void registerCloseable(Closeable closeableResource) {
		if (closeableResource == null) {
			AbstractCleanable.logger.debug("null was passed, which won't have any influence.");
			return;
		}

		this.closeableResources.add(closeableResource);
	}

	public void registerCleanable(Cleanable cleanableResource) {
		if (cleanableResource == null) {
			AbstractCleanable.logger.debug("null was passed, which won't have any influence.");
			return;
		}

		this.cleanableResources.add(cleanableResource);
	}

	public void cleanUp() {
		for (Closeable closeableResource : this.closeableResources) {
			try {
				closeableResource.close();
			} catch (IOException e) {
				AbstractCleanable.logger.warn("An IOException occurred while closing the following resource: " + closeableResource, e);
			}
		}

		for (Cleanable cleanableResource : this.cleanableResources) {
			cleanableResource.cleanUp();
		}

		this.closeableResources.clear();
	}
}
