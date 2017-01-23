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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>FilenameManager</code> manages all filenames within a directory. It will check if a filename does already exist within the directory. If
 * this is the case the <code>FilenameManager</code> will generate an alternative.
 * 
 * @author Matthias Pohl
 */
public class FilenameManager {

	private Map<String, Integer> filenameRegistry = new HashMap<String, Integer>();

	/**
	 * Initializes a <code>FilenameManager</code> for the passed directory.
	 * 
	 * @param directory
	 *            The directory that will be managed.
	 * @throws IllegalArgumentException
	 *             If a String was passed that is no directory path.
	 */
	public FilenameManager(String directory) {
		File dir = new File(directory);

		if (!dir.exists()) {
			dir.mkdir();
		}

		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("The passed String has to be a directory path.");
		}

		String[] filenames = dir.list();
		for (int i = 0; i < filenames.length; ++i) {
			this.addFilename(filenames[i]);
		}
	}

	private void addFilename(String filename) {
		if (this.filenameRegistry.containsKey(filename)) {
			int oldValue = this.filenameRegistry.get(filename);
			this.filenameRegistry.put(filename, ++oldValue);
		} else {
			this.filenameRegistry.put(filename, 0);
		}
	}

	/**
	 * Returns a valid alternative for the passed filename.
	 * 
	 * @param baseFilename
	 *            The file for which an alternative is requested. If the observed directory does not contain a file with such a name, the passed
	 *            filename will be the return value.
	 * @return A valid filename for the observed directory. This alternative can be the passed filename, if there is no file with the given name, or
	 *         the passed filename concatenated with an integer value.
	 */
	public String getNextValidFilename(String baseFilename) {
		this.addFilename(baseFilename);

		int ext = this.filenameRegistry.get(baseFilename);
		if (ext > 0) {
			return baseFilename + ext;
		}

		return baseFilename;
	}

}
