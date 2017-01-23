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
 * Created on Mar 19, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package genericEntity.util.bibtex.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * This implementation now features a buffer. This is more efficient than relying on BufferedReader since BufferedReader
 * is synchronized.
 * 
 * @author henkel
 */
final class LookAheadReader {
	private static final int BUFFERLEN = 512;

	/**
	 * Initializes a <code>LookAheadReader</code> that reads from <code>input</code>.
	 * 
	 * @param input
	 *            The reader that is used internally.
	 * @throws IOException
	 *             If an error occurs while reading.
	 */
	public LookAheadReader(Reader input) throws IOException {
		this.input = input;
		this.bufferPos = -1;
		this.bufferFilledUntil = 0;
		this.buffer = new char[LookAheadReader.BUFFERLEN];
		this.eof = false;
		this.line = 1;
		this.column = 0;
		step();
	}

	private final Reader input;
	private boolean eof;
	private int line, column;
	private char buffer[];
	private int bufferFilledUntil;
	private int bufferPos;

	/**
	 * Jumps to the next character.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading.
	 */
	public void step() throws IOException {
		if (this.eof)
			return;
		this.bufferPos++;
		if (this.bufferFilledUntil <= this.bufferPos) {
			this.bufferFilledUntil = this.input.read(this.buffer);
			if (this.bufferFilledUntil == -1) {
				this.eof = true;
				this.input.close();
			}
			this.bufferPos = 0;
		}
		char currentChar = this.buffer[this.bufferPos];
		if (currentChar == '\n') {
			this.line++;
			this.column = 0;
		} else {
			this.column++;
		}
	}

	/**
	 * Returns the current character.
	 * 
	 * @return The current character.
	 */
	public char getCurrent() {
		assert (!this.eof);
		return this.buffer[this.bufferPos];
	}

	/**
	 * Checks if the end-of-content was reached.
	 * 
	 * @return <code>true</code>, if the end-of-content was reached; otherwise <code>false</code>.
	 */
	public boolean eof() {
		return this.eof;
	}

	/**
	 * Returns the current line.
	 * 
	 * @return The current line.
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * Returns the current column.
	 * 
	 * @return The current column.
	 */
	public int getColumn() {
		return this.column;
	}
}