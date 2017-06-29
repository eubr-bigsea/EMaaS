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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.exception.ExtractionFailedException;
import genericEntity.util.Pair;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.json.JsonString;
import genericEntity.util.data.json.JsonUtil;
import genericEntity.util.data.json.JsonValue;

/**
 * <code>XMLSource</code> represents *.xml files.
 * 
 * @author Matthias Pohl
 */
public class XMLSource extends AbstractDataSource<XMLSource> implements Jsonable {

	/**
	 * <code>XMLSourceIterator</code> is used for generating {@link GenericObject}s out of <code>XMLSource</code>s.
	 * 
	 * @author Matthias Pohl
	 */
	protected class XMLSourceIterator extends AbstractDataSourceIterator<XMLSource> {

		private final XMLStreamReader reader;

		private boolean rootReached = false;

		private final Stack<Pair<String, JsonRecord>> elementStack = new Stack<Pair<String, JsonRecord>>();

		/**
		 * Initializes a <code>XMLSourceIterator</code> using the passed <code>XMLSource</code>.
		 * 
		 * @param source
		 *            The source of which the data shall be extracted.
		 * @throws XMLStreamException
		 *             If an error occurs while instantiating a new XML reader.
		 */
		protected XMLSourceIterator(XMLSource source) throws XMLStreamException {
			super(source);

			this.reader = this.dataSource.createXMLStreamReader();

			if (!this.dataSource.rootIsSet()) {
				this.rootReached = true;
			}
		}

		private void addToCurrentRecord(String localName, JsonValue obj) {
			final JsonRecord parent = this.elementStack.peek().getSecondElement();
			this.addAttributeValue(parent, localName, obj);
		}

		private boolean elementIsCurrentlyProcessed() {
			return !this.elementStack.isEmpty();
		}

		private void putNewRecordOnStack(String localName) {
			this.elementStack.push(new Pair<String, JsonRecord>(localName, new JsonRecord()));
		}

		@Override
		protected JsonRecord loadNextRecord() throws ExtractionFailedException {
			try {
				while (!this.rootReached && this.reader.hasNext()) {
					if (this.reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
						if (!this.rootReached && this.dataSource.rootIsSet()
								&& this.reader.getLocalName().equals(this.dataSource.getRootElementTag())) {
							this.rootReached = true;
						}
					}
				}
			} catch (final XMLStreamException e) {
				throw new ExtractionFailedException("Couldn't reach root element.", e);
			}

			// go to next start element tag
			int nextTag;
			do {
				try {
					nextTag = this.reader.nextTag();
				} catch (final XMLStreamException e) {
					// exception is raised if the current event is no white
					// space, start tag, end tag or processing
					// instruction
					return null;
				}
			} while (nextTag != XMLStreamConstants.START_ELEMENT);

			this.putNewRecordOnStack(this.reader.getLocalName());

			// check for attributes
			for (int i = 0; i < this.reader.getAttributeCount(); ++i) {
				this.addToCurrentRecord(this.reader.getAttributeLocalName(i), new JsonString(this.reader.getAttributeValue(i)));
			}

			final StringBuilder textContentBuffer = new StringBuilder();
			// start element processing
			try {
				while (this.elementIsCurrentlyProcessed() && this.reader.hasNext()) {
					switch (this.reader.next()) {
					case XMLStreamConstants.START_ELEMENT:
						// save text content
						String textContent = textContentBuffer.toString().trim();
						// clears the buffer
						textContentBuffer.setLength(0);

						if (textContent.length() > 0)
							this.addToCurrentRecord(this.elementStack.peek().getFirstElement(), new JsonString(textContent));

						this.putNewRecordOnStack(this.reader.getLocalName());

						// add all attributes
						for (int i = 0; i < this.reader.getAttributeCount(); ++i)
							this.addToCurrentRecord(this.reader.getAttributeLocalName(i), new JsonString(this.reader.getAttributeValue(i)));

						break;
					case XMLStreamConstants.ATTRIBUTE:
						for (int i = 0; i < this.reader.getAttributeCount(); ++i)
							this.addToCurrentRecord(this.reader.getAttributeLocalName(i), new JsonString(this.reader.getAttributeValue(i)));
						break;
					case XMLStreamConstants.CHARACTERS:
						// characters are added to the text content buffer
						textContentBuffer.append(this.reader.getText());
						break;
					case XMLStreamConstants.ENTITY_REFERENCE:
						// entity references are also added to the text content
						// buffer
						textContentBuffer.append(this.reader.getText());
						break;
					case XMLStreamConstants.END_ELEMENT:

						// end of root element was reached
						if (this.reader.getLocalName().equals(this.dataSource.getRootElementTag()))
							// end of observed data reached
							return null;

						textContent = textContentBuffer.toString().trim();
						// clears the buffer
						textContentBuffer.setLength(0);
						if (textContent.length() > 0)
							this.addToCurrentRecord(this.elementStack.peek().getFirstElement(), new JsonString(textContent));

						final Pair<String, JsonRecord> finishedElement = this.elementStack.pop();
						if (this.elementStack.isEmpty())
							return finishedElement.getSecondElement();

						if (finishedElement.getSecondElement().size() <= 1
								&& finishedElement.getSecondElement().containsKey(finishedElement.getFirstElement()))
							this.addToCurrentRecord(finishedElement.getFirstElement(),
									finishedElement.getSecondElement().get(finishedElement.getFirstElement()));
						else
							this.addToCurrentRecord(finishedElement.getFirstElement(), finishedElement.getSecondElement());

						break;
					}
				}
			} catch (final XMLStreamException e) {
				throw new ExtractionFailedException("Error occurred while parsing the XML file.", e);
			}

			if (this.elementIsCurrentlyProcessed()) {
				throw new ExtractionFailedException("Couldn't find end tag of the current element.");
			}

			return null;
		}

	}

	private static final Logger logger = Logger.getLogger(XMLSource.class.getPackage().getName());

	private static final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private String rootElement;

	private transient File xmlFile;

	// closing the readers cannot be handled by the Cleanable interface since XMLStreamReader does not implement Closeable
	private transient final Collection<XMLStreamReader> openedReaders = new ArrayList<XMLStreamReader>();

	/**
	 * Internal constructor for {@link Jsonable} deserialization.
	 */
	protected XMLSource() {
		// nothing to do
	}

	/**
	 * Initializes a <code>XMLSource</code> that converts all elements in the first XML layer into {@link JsonRecord}s.
	 * 
	 * @param identifier
	 *            The identifier of the {@link DataSource} instance.
	 * @param file
	 *            The XML file from which the data shall be extracted.
	 * @throws FileNotFoundException
	 *             If the passed file does not exist.
	 */
	public XMLSource(String identifier, File file) throws FileNotFoundException {
		this(identifier, file, null);
	}

	/**
	 * Initializes a <code>XMLSource</code> that converts all direct child elements of the given root into {@link GenericObject}s.
	 * 
	 * @param identifier
	 *            The identifier of the {@link DataSource} instance.
	 * @param file
	 *            The XML file from which the data shall be extracted.
	 * @param root
	 *            The tag of the element, of which the child elements will be extracted.
	 * @throws FileNotFoundException
	 *             If the passed file does not exist.
	 */
	public XMLSource(String identifier, File file, String root) throws FileNotFoundException {
		super(identifier);

		if (file == null) {
			throw new NullPointerException("The file is missing.");
		} else if (!file.exists()) {
			throw new FileNotFoundException("The file does not exist.");
		}

		this.rootElement = root;

		this.xmlFile = file;
	}

	/**
	 * Registers a {@link XMLStreamReader}. This reader will be closed during the next call of {@link #cleanUp()}.
	 * 
	 * @param reader
	 *            The statement that shall be closed later on.
	 */
	private void registerXMLStreamReader(XMLStreamReader reader) {
		if (reader == null) {
			XMLSource.logger.debug("null was passed, which won't have any influence.");
			return;
		}

		this.openedReaders.add(reader);
	}

	@Override
	public void cleanUp() {
		super.cleanUp();

		for (XMLStreamReader reader : this.openedReaders) {
			try {
				reader.close();
			} catch (XMLStreamException e) {
				XMLSource.logger.warn("An XMLStreamException occurred while closing an opened XMLStreamReader.", e);
			}
		}

		this.openedReaders.clear();
	}

	/**
	 * Instantiates a new {@link XMLStreamReader}.
	 * 
	 * @return The newly instantiated <code>XMLStreamReader</code>.
	 * @throws XMLStreamException
	 *             If an error occurred while instantiating the <code>XMLStreamReader</code>.
	 */
	protected XMLStreamReader createXMLStreamReader() throws XMLStreamException {
		XMLStreamReader reader;
		try {
			reader = XMLSource.inputFactory.createXMLStreamReader(new FileInputStream(this.xmlFile));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("The underlying file could not be found.", e);
		}

		this.registerXMLStreamReader(reader);
		return reader;
	}

	/**
	 * Checks whether a root element was set.
	 * 
	 * @return <code>true</code>, if a root element was set; otherwise <code>false</code>.
	 */
	protected boolean rootIsSet() {
		return this.rootElement != null;
	}

	/**
	 * Returns the set root element or <code>null</code>, if no root was set.
	 * 
	 * @return The set root element.
	 */
	protected String getRootElementTag() {
		return this.rootElement;
	}

	@Override
	public Iterator<GenericObject> iterator() {
		try {
			return new XMLSourceIterator(this);
		} catch (XMLStreamException e) {
			throw new IllegalStateException("An XMLStreamException occurred while initializing the XML reader.", e);
		}
	}

	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeRecordStart();
		JsonUtil.writeFields(jsonGenerator, this);
		jsonGenerator.writeRecordEntry("file", new JsonString(this.xmlFile.getCanonicalPath()));
		jsonGenerator.writeRecordEnd();
	}

	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		jsonParser.skipToken(JsonToken.START_OBJECT);
		JsonUtil.readFields(jsonParser, this);
		jsonParser.skipFieldName("file");
		this.xmlFile = new File(jsonParser.nextString());
		jsonParser.skipToken(JsonToken.END_OBJECT);
	}

}
