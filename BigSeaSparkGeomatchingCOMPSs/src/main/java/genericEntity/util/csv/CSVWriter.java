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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.log4j.Logger;

import genericEntity.util.GlobalConfig;

/**
 * <code>CSVWriter</code> generates <code>CSV</code> formatted data.
 * 
 * @author Matthias Pohl
 */
public class CSVWriter {

	private static final Logger logger = Logger.getLogger(CSVWriter.class.getPackage().getName());

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

	private transient Writer writer;

	private char separator = CSVWriter.DEFAULT_SEPARATOR;

	private char quoteCharacter = CSVWriter.DEFAULT_QUOTE_CHARACTER;

	private char escapeCharacter = CSVWriter.DEFAULT_ESCAPE_CHARACTER;

	private int columnCount = -1;

	/**
	 * Initializes a <code>CSVWriter</code> with the given {@link OutputStream}.
	 * 
	 * @param stream
	 *            The stream onto which this instance shall write.
	 */
	public CSVWriter(OutputStream stream) {
		if (stream == null) {
			throw new NullPointerException("OutputStream is missing.");
		}

		try {
			this.writer = new BufferedWriter(new OutputStreamWriter(stream, GlobalConfig.getInstance().getDefaultEncoding()));
		} catch (UnsupportedEncodingException e) {
			CSVWriter.logger
					.warn("The '" + GlobalConfig.getInstance().getDefaultEncoding() + "' encoding could not be used. The system's default encoding was chosen.", e);
			this.writer = new OutputStreamWriter(stream);
		}
	}

	/**
	 * Initializes a <code>CSVWriter</code> with the given {@link Writer}.
	 * 
	 * @param writer
	 *            The writer that is used for generating the output.
	 */
	public CSVWriter(Writer writer) {
		if (writer == null) {
			throw new NullPointerException("No writer was passed.");
		}

		this.writer = writer;
	}

	/**
	 * Quotes the passed String. Besides setting the quote character as a prefix and a suffix of the String; all occurrences within the String will be
	 * escaped.
	 * 
	 * @param str
	 *            The String that shall be passed.
	 * @return The quoted String. If <code>null</code> was passed, an empty String without quotes will be returned.
	 */
	protected String quote(String str) {
		if (str == null) {
			return "";
		}

		StringBuilder strBuilder = new StringBuilder();

		strBuilder.append(this.getQuoteCharacter());
		final String replacement = Character.toString(this.getEscapeCharacter()) + Character.toString(this.getQuoteCharacter());
		strBuilder.append(str.replaceAll(Character.toString(this.getQuoteCharacter()), replacement));
		strBuilder.append(this.getQuoteCharacter());

		return strBuilder.toString();
	}

	/**
	 * Writes the String into the output.
	 * 
	 * @param str
	 *            The String that shall be printed.
	 * @throws IOException
	 *             If an error occurs during the write process.
	 * @throws IllegalStateException
	 *             If the <code>CSVWriter</code> was already closed.
	 */
	protected void write(String str) throws IOException {
		if (this.writer == null) {
			throw new IllegalStateException("The CSVWriter was already closed.");
		}

		this.writer.write(str);
		this.writer.flush();
	}

	/**
	 * Adds a line break to the String and writes it into the output.
	 * 
	 * @param str
	 *            The String that shall be printed.
	 * @throws IOException
	 *             If an error occurs during the write process.
	 */
	protected void writeln(String str) throws IOException {
		this.write(str + System.getProperty("line.separator"));
	}

	/**
	 * Converts the String array into a valid CSV form.
	 * 
	 * @param data
	 *            The data that shall be converted.
	 * @return The CSV-formatted String.
	 */
	protected String generateDataLine(String... data) {
		if (data.length == 0) {
			// no data has to be added
			return "";
		}

		StringBuilder strBuilder = new StringBuilder();

		for (int i = 0; i < this.columnCount; ++i) {
			if (i > 0) {
				// add separator if first element was already appended
				strBuilder.append(this.getSeparator());
			}

			if (i < data.length) {
				// adds actual data
				strBuilder.append(this.quote(data[i]));
			} else {
				// data has less columns than the initial column count -> add empty columns
				strBuilder.append(this.quote(""));
			}
		}

		return strBuilder.toString();
	}

	/**
	 * Converts the passed data and writes the CSV-formatted String into the output.
	 * 
	 * @param data
	 *            The data that shall be printed.
	 * @throws IOException
	 *             If an error occurs during the write process.
	 */
	public void write(String... data) throws IOException {
		if (this.columnCount < 0) {
			// first call -> columnCount will be initialized
			this.columnCount = data.length;
		}

		// consistency check
		if (data.length > this.columnCount) {
			throw new IllegalArgumentException("The passed data has to much columns. Expected: " + this.columnCount
					+ "; Column count of current data: " + data.length);
		}

		this.writeln(this.generateDataLine(data));
	}

	/**
	 * Checks whether the writer was already closed.
	 * 
	 * @return <code>true</code>, if the writer was already closed; otherwise <code>false</code>.
	 */
	public boolean isClosed() {
		return this.writer == null;
	}

	/**
	 * Closes the underlying writer.
	 * 
	 * @throws IOException
	 *             If an error occurs during closing.
	 */
	public void close() throws IOException {
		if (this.isClosed()) {
			CSVWriter.logger.warn("CSVReader was not closed since it is already closed.");
			return;
		}

		this.writer.close();
		this.writer = null;
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
