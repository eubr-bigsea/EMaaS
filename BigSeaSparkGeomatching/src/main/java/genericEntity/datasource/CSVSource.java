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

package genericEntity.datasource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.exception.ExtractionFailedException;
import genericEntity.util.csv.CSVReader;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;
import genericEntity.util.data.json.JsonNull;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.json.JsonString;
import genericEntity.util.data.json.JsonUtil;
import genericEntity.util.data.json.JsonValue;

/**
 * <code>CSVSource</code> represents *.csv files.
 * 
 * @author Matthias Pohl
 */
public class CSVSource extends AbstractDataSource<CSVSource> implements Jsonable {

	/**
	 * <code>CSVSourceIterator</code> is used for generating {@link GenericObject}s out of <code>CSVSource</code>s.
	 * 
	 * @author Ziawasch Abedjan
	 * @author Matthias Pohl
	 */
	protected class CSVSourceIterator extends AbstractDataSourceIterator<CSVSource> {

		private final CSVReader csvParser;

		private String[] attrNames;

		/**
		 * Initializes a <code>CSVSourceIterator</code> using the passed <code>CSVSource</code>.
		 * 
		 * @param source
		 *            The source of which the data shall be extracted.
		 */
		protected CSVSourceIterator(CSVSource source) {
			super(source);
			this.csvParser = source.createCSVReaderInstance();
			this.attrNames = source.getColumnNames();

			if (this.csvParser == null) {
				throw new IllegalStateException("The CSVReader could not be initialized.");
			}

			if (this.dataSource.readHeader()) {
				try {
					this.attrNames = this.csvParser.nextLine();
				} catch (final IOException e) {
					throw new ExtractionFailedException("IOExeption occurred during header parsing.", e);
				} catch (final ParseException e) {
					throw new ExtractionFailedException("ParseException occurred during header parsing.", e);
				}
			} else if (this.attrNames != null) {
				this.csvParser.setColumnCount(this.attrNames.length);
			}
		}

		private void close() {
			try {
				this.csvParser.close();
			} catch (final IOException e) {
				CSVSource.logger.warn("Error occurred while closing the CSV file '" + this.dataSource.getFilePath() + "'.");
			}
		}

		@Override
		protected JsonRecord loadNextRecord() throws ExtractionFailedException {
			if (this.csvParser.isClosed()) {
				return null;
			}

			JsonRecord retVal = null;
			try {
				final String[] data = this.csvParser.nextLine();

				if (data == null) {
					// end of file reached
					this.close();
					return null;
				}

				// set column names, if its not already done
				if (this.attrNames == null) {
					this.attrNames = new String[this.csvParser.getColumnCount()];

					for (int i = 0; i < this.attrNames.length; ++i) {
						this.attrNames[i] = String.valueOf(i);
					}
				}

				retVal = new JsonRecord();
				for (int i = 0; i < this.csvParser.getColumnCount(); i++) {
					JsonValue value;
					if (data[i] == null) {
						value = JsonNull.NULL;
					} else {
						value = new JsonString(data[i]);
					}

					this.addAttributeValue(retVal, this.attrNames[i].intern(), value);
				}

			} catch (final IOException e) {
				CSVSource.logger.error("Error while reading from the CSV file '" + this.dataSource.getFilePath() + "'.", e);
				this.close();

				return null;
			} catch (final ParseException e) {
				CSVSource.logger.error("Parsing error while reading from the CSV file '" + this.dataSource.getFilePath() + "'.", e);
				this.close();

				return null;
			}

			return retVal;
		}

	}

	private static final Logger logger = Logger.getLogger(CSVSource.class.getPackage().getName());

	private char quoteChar = '"';
	private char separatorChar = ';';

	private String[] columnNames;

	private boolean readHeader = false;

	private transient File csvFile;

	/**
	 * Internal constructor for {@link Jsonable} deserialization.
	 */
	protected CSVSource() {
		super();
	}

	/**
	 * Initializes a <code>CSVSource</code>.
	 * 
	 * @param identifier
	 *            The identifier of the {@link DataSource} instance.
	 * @param file
	 *            The file of which the data will be extracted.
	 * @throws FileNotFoundException
	 *             If the passed file does not exist.
	 */
	public CSVSource(String identifier, File file) throws FileNotFoundException {
		this(identifier, file, (String[]) null);
	}

	/**
	 * Initializes a <code>CSVSource</code> with column names.
	 * 
	 * @param identifier
	 *            The identifier of the {@link DataSource} instance.
	 * @param file
	 *            The file of which the data will be extracted.
	 * @param colNames
	 *            The column names.
	 * @throws FileNotFoundException
	 *             If the passed file does not exist.
	 */
	public CSVSource(String identifier, File file, String... colNames) throws FileNotFoundException {
		super(identifier);

		if (file == null) {
			throw new NullPointerException("No file was passed.");
		}

		if (!file.exists()) {
			throw new FileNotFoundException("'" + file.getName() + "' does not exist.");
		}

		this.csvFile = file;
		this.columnNames = colNames;
	}

	/**
	 * Returns the set quote character.
	 * 
	 * @return The set quote character.
	 */
	public char getQuoteCharacter() {
		return this.quoteChar;
	}

	/**
	 * Sets the quote character.
	 * 
	 * @param quoteChar
	 *            The quote character to set.
	 */
	public void setQuoteCharacter(char quoteChar) {
		this.quoteChar = quoteChar;
	}

	/**
	 * Returns the set separator character.
	 * 
	 * @return The set separator character.
	 */
	public char getSeparatorCharacter() {
		return this.separatorChar;
	}

	/**
	 * Sets the separator character.
	 * 
	 * @param separatorChar
	 *            The separator character to set.
	 */
	public void setSeparatorCharacter(char separatorChar) {
		this.separatorChar = separatorChar;
	}

	/**
	 * Disables header reading. The flag needs to be disabled, if the first line does not contain the column names.
	 */
	public void disableHeader() {
		this.readHeader = false;
	}

	/**
	 * Enables header reading. The flag needs to be enabled, if the first line contains the column names.
	 */
	public void enableHeader() {
		this.readHeader = true;
	}

	/**
	 * Checks whether interpreting the first line as header is enabled.
	 * 
	 * @return <code>true</code>, if the header flag is enabled; otherwise <code>false</code>.
	 */
	public boolean readHeader() {
		return this.readHeader;
	}

	/**
	 * By calling this method the extractor will interpret the first line as a header.
	 * 
	 * @return The current instance.
	 * 
	 * @see #enableHeader()
	 */
	public CSVSource withHeader() {
		this.enableHeader();
		return this;
	}

	/**
	 * Set the quote character that is used in the file.
	 * 
	 * @param quChar
	 *            The quote character.
	 * @return The current instance.
	 * 
	 * @see #setQuoteCharacter(char)
	 */
	public CSVSource withQuoteCharacter(char quChar) {
		this.setQuoteCharacter(quChar);
		return this;
	}

	/**
	 * Sets the separator character that is used in the file.
	 * 
	 * @param sepChar
	 *            The separator character.
	 * @return The current instance.
	 * 
	 * @see #setSeparatorCharacter(char)
	 */
	public CSVSource withSeparatorCharacter(char sepChar) {
		this.setSeparatorCharacter(sepChar);
		return this;
	}

	@Override
	public Iterator<GenericObject> iterator() {
		return new CSVSourceIterator(this);
	}

	private CSVReader createCSVReaderInstance() {
		try {
			final CSVReader reader = new CSVReader(new FileReader(this.csvFile));
			reader.setSeparator(this.getSeparatorCharacter());
			reader.setQuoteCharacter(this.getQuoteCharacter());
			this.registerCloseable(reader);
			return reader;
		} catch (final FileNotFoundException e) {
			CSVSource.logger.error("'" + this.csvFile.getName() + "' does not exist.");
			return null;
		}
	}

	/**
	 * Returns the path of the CSV file.
	 * 
	 * @return The path of the CSV file.
	 */
	protected String getFilePath() {
		return this.csvFile.getAbsolutePath();
	}

	/**
	 * Returns the column names or <code>null</code>, if no column names were set.
	 * 
	 * @return The column names.
	 */
	public String[] getColumnNames() {
		return this.columnNames;
	}

	@Override
	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeRecordStart();
		JsonUtil.writeFields(jsonGenerator, this);
		jsonGenerator.writeRecordEntry("file", new JsonString(this.csvFile.getCanonicalPath()));
		jsonGenerator.writeRecordEnd();
	}

	@Override
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		jsonParser.skipToken(JsonToken.START_OBJECT);
		JsonUtil.readFields(jsonParser, this);
		jsonParser.skipFieldName("file");
		this.csvFile = new File(jsonParser.nextString());
		jsonParser.skipToken(JsonToken.END_OBJECT);
	}

}
