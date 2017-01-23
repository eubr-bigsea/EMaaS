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

import genericEntity.datasource.DataSource;
import genericEntity.util.data.GenericObject;

/**
 * This exception is thrown by runtime if an identifier is used more than one
 * time (e.g. by {@link GenericObject} or {@link DataSource}).
 * 
 * @author Matthias Pohl
 */
public class IdentifierIsAlreadyInUseException extends RuntimeException {

	private static final long serialVersionUID = 3474815765876641262L;

	/**
	 * Calls {@link RuntimeException#RuntimeException()}
	 */
	public IdentifierIsAlreadyInUseException() {
		super();
	}

	/**
	 * Calls {@link RuntimeException#RuntimeException(String)}
	 * 
	 * @param msg
	 *            The error message.
	 */
	public IdentifierIsAlreadyInUseException(String msg) {
		super(msg);
	}

	/**
	 * Calls {@link RuntimeException#RuntimeException(Throwable)}
	 * 
	 * @param cause
	 *            The exception that caused this exception.
	 */
	public IdentifierIsAlreadyInUseException(Throwable cause) {
		super(cause);
	}

	/**
	 * Calls {@link RuntimeException#RuntimeException(String, Throwable)}
	 * 
	 * @param msg
	 *            The error message.
	 * @param cause
	 *            The exception that caused this exception.
	 */
	public IdentifierIsAlreadyInUseException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
