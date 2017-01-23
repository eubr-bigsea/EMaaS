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
 * Created on Mar 29, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package genericEntity.util.bibtex.expander;

import genericEntity.util.bibtex.data.BibtexFile;

/**
 * An expander is a transformer that makes a bibtex model more elaborate.
 * 
 * @author henkel
 */
public interface Expander {

	public void expand(BibtexFile file) throws ExpansionException;

	/**
	 * @return this method returns all exceptions that have been accumulated in
	 *         the last call to expand. Whether or not expand accumulates
	 *         exceptions depends on the configuration of the expander, which
	 *         is usually specified in the constructor call by setting the flag
	 *         throwAllExpansionExceptions. Oviously, implementers of this
	 *         interface should provide this flag as a parameter to the
	 *         constructor. Hint: Extend AbstractExpander.
	 */
	public ExpansionException[] getExceptions();
}
