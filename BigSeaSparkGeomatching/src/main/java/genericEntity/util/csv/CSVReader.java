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

package genericEntity.util.csv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * <code>CSVReader</code> reads <code>CSV</code> formatted data.
 * 
 * @author Matthias Pohl
 */
public class CSVReader implements Closeable {

	/**
	 * The default separator character.
	 */
	public static final char DEFAULT_SEPARATOR = ';';

	/**
	 * The default quote character.
	 */
	public static final char DEFAULT_QUOTE_CHARACTER = '"';

	/**
	 * The default escape character.
	 */
	public static final char DEFAULT_ESCAPE_CHARACTER = '\\';

	private BufferedReader reader;
	private int columnCount = -1;

	private char separator = CSVReader.DEFAULT_SEPARATOR;

	private char quoteCharacter = CSVReader.DEFAULT_QUOTE_CHARACTER;

	private char escapeCharacter = CSVReader.DEFAULT_ESCAPE_CHARACTER;

	/**
	 * Initializes a <code>CSVReader</code> with the given {@link Reader}.
	 * 
	 * @param reader
	 *            The reader that is used internally.
	 */
	public CSVReader(Reader reader) {
		if (reader == null) {
			throw new NullPointerException("No reader was passed.");
		}

		this.reader = new BufferedReader(reader);
	}

	/**
	 * Returns the number of columns of the currently processed data source. Actually this is the column count of the first row.
	 * 
	 * @return The over-all column count of data source.
	 */
	public int getColumnCount() {
		return this.columnCount;
	}

	/**
	 * Sets the column count. All rows that will be read have to have this column count.
	 * 
	 * @param columnCount
	 *            The column count.
	 */
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	/**
	 * Checks whether the passed character can be escaped. Valid characters are the set separator, quote, and escape characters as well as all
	 * characters, that are allowed to escape in Java ('n', 'r', 't', 'r', 'f', ''', '"', '\').
	 * 
	 * @param ch
	 *            The character that shall be checked.
	 * @return <code>true</code>, if the passed character can be escaped; otherwise <code>false</code>.
	 */
	protected boolean canBeEscaped(char ch) {
		return ch == 'n' || ch == 't' || ch == 'b' || ch == 'f' || ch == 'r' || ch == '"' || ch == '\'' || ch == '\\'
				|| ch == this.getQuoteCharacter() || ch == this.getEscapeCharacter() || ch == this.getSeparator();
	}

	/**
	 * Extracts the data out of the passed String.
	 * 
	 * @param str
	 *            The String that shall be parsed.
	 * @return An array of Strings containing the actual values of the current line.
	 * @throws ParseException
	 *             If an error occurs due to a wrong format.
	 */
	protected String[] getDataLine(String str) throws ParseException {
		Collection<String> data = new ArrayList<String>();

		char[] charArray = str.toCharArray();

		boolean escaped = false; // checks whether the last character was an escape character
		boolean inQuotes = false; // checks whether the current value is quoted
		StringBuilder currentValueBuilder = new StringBuilder(); // stores the current value
		for (int i = 0; i < charArray.length; ++i) {

			// if (escaped) { // check valid escape constraints
			// if (!this.canBeEscaped(charArray[i])) {
			// System.err.println(new String(charArray));
			// throw new ParseException("Invalid character escape for character '" + charArray[i] + "'.", -1);
			// }
			// }

			if (charArray[i] == this.getSeparator() && (!inQuotes && !escaped)) { // separator
				if ((i == 0) || (charArray[i - 1] == this.getSeparator())) {
					data.add(null);
				} else {
					data.add(currentValueBuilder.toString());
				}

				currentValueBuilder = new StringBuilder();
			} else if (charArray[i] == this.getQuoteCharacter()) { // quote character
				if (inQuotes) {
					if (escaped) {
						currentValueBuilder.append(charArray[i]);
					} else {
						inQuotes = false;
					}
				} else {
					if (escaped) {
						currentValueBuilder.append(charArray[i]);
					} else {
						if (currentValueBuilder.length() == 0) {
							inQuotes = true;
						} else {
							currentValueBuilder.append(charArray[i]);
						}
					}
				}
			} else if (charArray[i] != this.getEscapeCharacter() || escaped) { // all other characters
				currentValueBuilder.append(charArray[i]);
			}

			if (charArray[i] == this.getEscapeCharacter() && !escaped) { // escape character
				escaped = true;
			} else {
				escaped = false;
			}
		}

		if (inQuotes) { // all quotes should have been closed already
			throw new ParseException("Quoting error at the end of the following line: " + str, -1);
		}

		if (escaped) { // escape character without an character to escape are invalid
			throw new ParseException("Escape error at the end of the following line: " + str, -1);
		}

		if ((charArray.length == 0) || (charArray[charArray.length - 1] == this.getSeparator())) {
			data.add(null);
		} else {
			data.add(currentValueBuilder.toString());
		}

		return data.toArray(new String[data.size()]);
	}

	/**
	 * Returns the data of the next line or <code>null</code>, if the end of the data source was reached.
	 * 
	 * @return The data of the next line or <code>null</code>, if the end of the data source was reached.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 * @throws ParseException
	 *             If an error occurs due to wrong formats within the next line.
	 * @throws IllegalStateException
	 *             If the <code>CSVReader</code> was already closed.
	 */
	public String[] nextLine() throws IOException, ParseException {
		if (this.reader == null) {
			throw new IllegalStateException("The CSVReader was already closed.");
		}

		final String currentLine = this.reader.readLine();

		if (currentLine == null) {
			return null;
		}

		String[] returnValue = this.getDataLine(currentLine);

		if (this.getColumnCount() < 0) {
			this.setColumnCount(returnValue.length);
		} else if (this.getColumnCount() != returnValue.length) {
			throw new IllegalStateException("The following line has an invalid number of columns (" + returnValue.length + "; expected: "
					+ this.getColumnCount() + "): " + currentLine);
		}

		return returnValue;
	}

	/**
	 * Checks whether the reader was already closed.
	 * 
	 * @return <code>true</code>, if the reader was already closed; otherwise <code>false</code>.
	 */
	public boolean isClosed() {
		return this.reader == null;
	}

	/**
	 * Closes the internally used reader.
	 * 
	 * @throws IOException
	 *             If an error occurs during the closing process.
	 */
	@Override
	public void close() throws IOException {
		if (this.isClosed()) {
			return;
		}

		this.reader.close();
		this.reader = null;
	}

	/**
	 * Returns the current separator character.
	 * 
	 * @return The current separator character.
	 */
	public char getSeparator() {
		return this.separator;
	}

	/**
	 * Sets the separator character.
	 * 
	 * @param separator
	 *            The separator character to set.
	 */
	public void setSeparator(char separator) {
		this.separator = separator;
	}

	/**
	 * Returns the current quote character.
	 * 
	 * @return The current quote character.
	 */
	public char getQuoteCharacter() {
		return this.quoteCharacter;
	}

	/**
	 * Sets the quote character.
	 * 
	 * @param quoteCharacter
	 *            The quote character to set.
	 */
	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	/**
	 * Returns the current escape character.
	 * 
	 * @return The current escape character.
	 */
	public char getEscapeCharacter() {
		return this.escapeCharacter;
	}

	/**
	 * Sets the escape character.
	 * 
	 * @param escapeCharacter
	 *            The escape character to set.
	 */
	public void setEscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}
}
