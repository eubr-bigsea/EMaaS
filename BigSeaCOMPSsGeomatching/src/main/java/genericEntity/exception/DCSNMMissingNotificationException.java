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

package genericEntity.exception;

import java.util.NoSuchElementException;

/**
 * This exception should be raised by {@link DuplicateCountSNM}, if
 * the algorithm has not been notified of the comparison result of the
 * latest pair.  
 * 
 * @author Fabian Lindenberg
 */
public class DCSNMMissingNotificationException extends NoSuchElementException  {

	private static final long serialVersionUID = 2663722114971825850L;

	private Throwable cause;

	/**
	 * Calls {@link NoSuchElementException#NoSuchElementException()}.
	 */
	public DCSNMMissingNotificationException() {
		super();
	}

	/**
	 * Calls {@link NoSuchElementException#NoSuchElementException(String)}.
	 * 
	 * @param msg
	 *            The error message.
	 */
	public DCSNMMissingNotificationException(String msg) {
		super(msg);
	}

	/**
	 * Calls {@link NoSuchElementException#NoSuchElementException()} and stores
	 * the passed cause.
	 * 
	 * @param cause
	 *            The exception that caused this exception.
	 */
	public DCSNMMissingNotificationException(Throwable cause) {
		super();
		this.cause = cause;
	}

	/**
	 * Calls {@link NoSuchElementException#NoSuchElementException(String)} and
	 * stores the passed cause.
	 * 
	 * @param msg
	 *            The error message.
	 * @param cause
	 *            The exception that caused this exception.
	 */
	public DCSNMMissingNotificationException(String msg, Throwable cause) {
		super(msg);
		this.cause = cause;
	}

	/**
	 * @see java.lang.Throwable#getCause()
	 */
	@Override
	public Throwable getCause() {
		return this.cause;
	}
	
}
