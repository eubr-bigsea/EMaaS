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

/*
 * Created on Oct 29, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package genericEntity.util.bibtex.expander;

import java.util.LinkedList;

/**
 * @author henkel
 */
public abstract class AbstractExpander {

	/**
	 * @param throwAllExpansionExceptions
	 *            Setting this to true means that all exceptions will be thrown
	 *            immediately. Otherwise, the expander will skip over things it
	 *            can't expand and you can use getExceptions to retrieve the
	 *            exceptions later
	 */

	protected AbstractExpander(boolean throwAllExpansionExceptions) {
		this.throwAllExpansionExceptions = throwAllExpansionExceptions;
		this.exceptions = throwAllExpansionExceptions ? null : new LinkedList<ExpansionException>();
	}

	private final boolean throwAllExpansionExceptions;

	/*
	 * (non-Javadoc)
	 * 
	 * @see bibtex.expansions.Expander#getExceptions()
	 */
	public ExpansionException[] getExceptions() {
		return this.exceptionsAsArrays;
	}

	/**
	 * Call this at the end of your expand(BibtexFile) implementation.
	 */
	protected void finishExpansion() {
		if(this.exceptions==null) return;
		this.exceptionsAsArrays = new ExpansionException[this.exceptions.size()];
		this.exceptions.toArray(this.exceptionsAsArrays);
		this.exceptions.clear();
	}

	/**
	 * Call this whenever you want to throw an ExpansionException. This method may or may not throw
	 * the exception, depending on the throwAllExpansionExceptions flag.
	 * 
	 * @param message
	 * @throws ExpansionException
	 */
	protected void throwExpansionException(String message) throws ExpansionException {
		if (this.throwAllExpansionExceptions) {
			throw new ExpansionException(message);
		}
		
		try {
			throw new ExpansionException(message);
		} catch (ExpansionException e) {
			this.exceptions.add(e);
		}
	}

	/**
	 * Call this whenever you want to throw an ExpansionException. This method may or may not throw
	 * the exception, depending on the throwAllExpansionExceptions flag.
	 * 
	 * @param cause
	 * @throws ExpansionException
	 */
	protected void throwExpansionException(Exception cause) throws ExpansionException {
		if (this.throwAllExpansionExceptions) {
			throw new ExpansionException(cause);
		}
		
		try {
			throw new ExpansionException(cause);
		} catch (ExpansionException e) {
			this.exceptions.add(e);
		}
	}

	
	private final LinkedList<ExpansionException> exceptions;
	private ExpansionException[] exceptionsAsArrays;

}
