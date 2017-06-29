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

/**
 * <code>MemoryChecker</code> is a Singleton implementation, that maintains the memory usage.
 * 
 * @author Matthias Pohl
 */
public class MemoryChecker {
	/**
	 * Singleton instance.
	 */
	private static MemoryChecker instance = null;

	private double maximumMemoryUsage = GlobalConfig.getInstance().getMaximumMemoryUsage();

	private long snapshotCount = 0;
	private final long freeMemoryAtStart = MemoryChecker.getFreeVMMemory();

	/**
	 * Initializes the singleton instance.
	 */
	protected MemoryChecker() {
		// nothing to do
	}

	/**
	 * Singleton method for returning the Singleton instance.
	 * 
	 * @return The <code>MemoryChecker</code> Singleton instance.
	 */
	public static MemoryChecker getInstance() {
		if (MemoryChecker.instance == null) {
			MemoryChecker.instance = new MemoryChecker();
//			MemoryChecker.instance.makeMemoryUsageSnapshot();
		}

		return MemoryChecker.instance;
	}

	private static long getFreeVMMemory() {
		return Runtime.getRuntime().freeMemory();
	}

	/**
	 * Returns the amount of already used memory.
	 * 
	 * @return The amount of already used memory.
	 */
	public static long getUsedVMMemory() {
		// total amount is the memory that is already allocated by the VM
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	/**
	 * Returns the maximum amount of memory that can be used.
	 * 
	 * @return The absolute number of bytes that can be used.
	 */
	public long getMaximumMemory() {
		return new Double(this.getMaximumMemoryUsage() * Runtime.getRuntime().maxMemory()).longValue();
	}

	private double getAverageMemoryUsage() {
		if (this.snapshotCount == 0 || this.freeMemoryAtStart < MemoryChecker.getFreeVMMemory()) {
			return 0;
		}

		return ((double) MemoryChecker.getUsedVMMemory() / this.snapshotCount);
	}

	/**
	 * Sets the maximum memory usage in percent.
	 * 
	 * @param maxUsage
	 *            The maximum usage.
	 */
	public void setMaximumMemoryUsage(double maxUsage) {
		if (maxUsage < 0.0) {
			this.maximumMemoryUsage = 0.0;
		} else if (maxUsage > 1.0) {
			this.maximumMemoryUsage = 1.0;
		} else {
			this.maximumMemoryUsage = maxUsage;
		}
	}

	/**
	 * Returns the maximum memory usage in percent.
	 * 
	 * @return The maximum memory usage in percent.
	 */
	public double getMaximumMemoryUsage() {
		return this.maximumMemoryUsage;
	}

	/**
	 * Checks the memory usage status and makes a statistical snapshot. These statistics can be used to calculate the average memory usage.
	 */
	public void makeMemoryUsageSnapshot() {
		++this.snapshotCount;
	}

	/**
	 * Checks whether there is enough memory available. This decision is made by using the statistics gathered by calling
	 * {@link #makeMemoryUsageSnapshot()}. This method returns <code>true</code> , if the average memory usage value fits into memory; otherwise
	 * <code>false</code>.
	 * 
	 * @return <code>true</code>, if enough memory is available; otherwise <code>false</code>.
	 */
	public boolean enoughMemoryAvailable() {
		if (GlobalConfig.getInstance().getInMemoryObjectThreshold() > 0) {
			return this.snapshotCount % GlobalConfig.getInstance().getInMemoryObjectThreshold() != 0;
		}

		// memory check if step size is 0 (experimental!)
		return this.getAverageMemoryUsage() < this.getMaximumMemory() - MemoryChecker.getUsedVMMemory();
	}

	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		
		strBuilder.append("Free Memory:\t\t").append(MemoryChecker.getFreeVMMemory()).append(" bytes").append(System.getProperty("line.separator"));
		strBuilder.append("Used Memory:\t\t").append(MemoryChecker.getUsedVMMemory()).append(" bytes").append(System.getProperty("line.separator"));
		strBuilder.append("Total Memory:\t\t").append(this.getMaximumMemory()).append(" bytes").append(System.getProperty("line.separator"));
		strBuilder.append("Average Memory Usage:\t").append(this.getAverageMemoryUsage()).append(" bytes").append(System.getProperty("line.separator"));
		
		return strBuilder.toString();
	}
}
