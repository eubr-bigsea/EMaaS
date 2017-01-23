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
 * Created on Mar 22, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * With standard macros we mean the ones defined in plain.bst
 * 
 * @author henkel
 */
public final class BibtexStandardMacros {

	private static HashSet<String> monthAbbr = new HashSet<String>();
	static {
		monthAbbr.addAll(Arrays.asList(new String[] { "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep",
				"oct", "nov", "dec" }));
	}

	/**
	 * Checks whether the passed String is a standard macro key for month names.
	 * 
	 * @param string
	 *            The String that shall be checked.
	 * @return <code>true</code>, if it is a standard macro key for month names; otherwise <code>false</code>.
	 */
	public static boolean isMonthAbbreviation(String string) {
		return monthAbbr.contains(string);
	}

	private static HashMap<String, String> standardMacros = new HashMap<String, String>();
	static {
		standardMacros.put("jan", "January");
		standardMacros.put("feb", "February");
		standardMacros.put("mar", "March");
		standardMacros.put("apr", "April");
		standardMacros.put("may", "May");
		standardMacros.put("jun", "June");
		standardMacros.put("jul", "July");
		standardMacros.put("aug", "August");
		standardMacros.put("sep", "September");
		standardMacros.put("oct", "October");
		standardMacros.put("nov", "November");
		standardMacros.put("dec", "December");
		standardMacros.put("acmcs", "ACM Computing Surveys");
		standardMacros.put("acta", "Acta Informatica");
		standardMacros.put("cacm", "Communications of the ACM");
		standardMacros.put("ibmjrd", "IBM Journal of Research and Development");
		standardMacros.put("ibmsj", "IBM Systems Journal");
		standardMacros.put("ieeese", "IEEE Transactions on Software Engineering");
		standardMacros.put("ieeetc", "IEEE Transactions on Computers");
		standardMacros.put("ieeetcad", "IEEE Transactions on Computer-Aided Design of Integrated Circuits");
		standardMacros.put("ipl", "Information Processing Letters");
		standardMacros.put("jacm", "Journal of the ACM");
		standardMacros.put("jcss", "Journal of Computer and System Sciences");
		standardMacros.put("scp", "Science of Computer Programming");
		standardMacros.put("sicomp", "SIAM Journal on Computing");
		standardMacros.put("tocs", "ACM Transactions on Computer Systems");
		standardMacros.put("tods", "ACM Transactions on Database Systems");
		standardMacros.put("tog", "ACM Transactions on Graphics");
		standardMacros.put("toms", "ACM Transactions on Mathematical Software");
		standardMacros.put("toois", "ACM Transactions on Office Information Systems");
		standardMacros.put("toplas", "ACM Transactions on Programming Languages and Systems");
		standardMacros.put("tcs", "Theoretical Computer Science");
	}

	/**
	 * Checks whether the passed String is the name of a standard macro.
	 * 
	 * @param macroName
	 *            The String that shall be checked.
	 * @return <code>true</code>, if it is the name of a standard macro; otherwise <code>false</code>.
	 */
	public static boolean isStandardMacro(String macroName) {
		return BibtexStandardMacros.standardMacros.containsKey(macroName);
	}

	/**
	 * Returns the value corresponding to the passed standard macro name.
	 * 
	 * @param macroName
	 *            The name of the standard macro whose value shall be returned.
	 * @return The value of the standard macro or <code>null</code>, if the passed String is no standard macro.
	 */
	public static String resolveStandardMacro(String macroName) {
		return standardMacros.get(macroName);
	}
}
