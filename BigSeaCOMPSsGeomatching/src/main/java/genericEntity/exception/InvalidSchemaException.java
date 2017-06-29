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

/**
 * This Exception can be thrown in every schema-related case.
 * 
 * @author Matthias Pohl
 */
public class InvalidSchemaException extends Exception {

	private static final long serialVersionUID = -4555727711894248657L;

	/**
	 * Calls {@link Exception#Exception()}.
	 */
	public InvalidSchemaException() {
		super();
	}

	/**
	 * Calls {@link Exception#Exception(String)}.
	 * 
	 * @param msg
	 *            The error message.
	 */
	public InvalidSchemaException(String msg) {
		super(msg);
	}

	/**
	 * Calls {@link Exception#Exception(Throwable)}.
	 * 
	 * @param cause
	 *            The exception that caused this exception.
	 */
	public InvalidSchemaException(Throwable cause) {
		super(cause);
	}

	/**
	 * Calls {@link Exception#Exception(String, Throwable)}.
	 * 
	 * @param msg
	 *            The error message.
	 * @param cause
	 *            The exception that caused this exception.
	 */
	public InvalidSchemaException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
