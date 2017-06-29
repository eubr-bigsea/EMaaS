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

import java.util.TimerTask;

/**
 * This task retrieves the memory usage of the current process and stores the maximum memory usage ever occurred.
 * 
 * @author ziawasch.abedjan
 * 
 */
public class MemoryCheckerTask extends TimerTask {
	private long maxMemoryUsed = 0;
	private long samplingFrequency = 0;
	private long totalMemoryUsed = 0;
	private long minMemoryUsed = 0;

	/**
	 * Gets the maximum amount of memory used by the current application.
	 * 
	 * @return Maximum amount of memory in KB.
	 */
	public long getMaxMemoryUsed() {
		return this.maxMemoryUsed / 1024;
	}

	@Override
	public void run() {
		long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		this.maxMemoryUsed = this.maxMemoryUsed > memory ? this.maxMemoryUsed : memory;
		this.minMemoryUsed = this.minMemoryUsed < memory && this.minMemoryUsed != 0 ? this.minMemoryUsed : memory;
		this.totalMemoryUsed = this.totalMemoryUsed + memory / 1024L;
		this.samplingFrequency++;
	}

	/**
	 * Gets the average amount of memory used by the current application.
	 * 
	 * @return Maximum amount of memory in KB.
	 */
	public long getAverageMemoryUsed() {
		if (this.samplingFrequency == 0) {
			return 0;
		}
		return this.totalMemoryUsed / this.samplingFrequency;
	}

	/**
	 * Gets the minimum amount of memory used by the current application.
	 * 
	 * @return Minimum amount of memory in KB.
	 */
	public long getMinMemoryUsed() {
		return this.minMemoryUsed / 1024;
	}

}
