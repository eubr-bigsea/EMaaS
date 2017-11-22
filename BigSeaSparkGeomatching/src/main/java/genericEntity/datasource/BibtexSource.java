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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonToken;

import genericEntity.exception.ExtractionFailedException;
import genericEntity.util.GlobalConfig;
import genericEntity.util.bibtex.data.BibtexAbstractEntry;
import genericEntity.util.bibtex.data.BibtexAbstractValue;
import genericEntity.util.bibtex.data.BibtexConcatenatedValue;
import genericEntity.util.bibtex.data.BibtexEntry;
import genericEntity.util.bibtex.data.BibtexFile;
import genericEntity.util.bibtex.data.BibtexMacroDefinition;
import genericEntity.util.bibtex.data.BibtexMacroReference;
import genericEntity.util.bibtex.data.BibtexPerson;
import genericEntity.util.bibtex.data.BibtexPersonList;
import genericEntity.util.bibtex.data.BibtexPreamble;
import genericEntity.util.bibtex.data.BibtexStandardMacros;
import genericEntity.util.bibtex.data.BibtexString;
import genericEntity.util.bibtex.data.BibtexToplevelComment;
import genericEntity.util.bibtex.expander.CrossReferenceExpander;
import genericEntity.util.bibtex.expander.ExpansionException;
import genericEntity.util.bibtex.expander.MacroReferenceExpander;
import genericEntity.util.bibtex.expander.PersonListExpander;
import genericEntity.util.bibtex.parser.BibtexParser;
import genericEntity.util.bibtex.parser.ParseException;
import genericEntity.util.data.GenericObject;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;
import genericEntity.util.data.json.JsonArray;
import genericEntity.util.data.json.JsonBoolean;
import genericEntity.util.data.json.JsonNull;
import genericEntity.util.data.json.JsonRecord;
import genericEntity.util.data.json.JsonString;
import genericEntity.util.data.json.JsonUtil;
import genericEntity.util.data.json.JsonValue;

/**
 * <code>BibtexSource</code> represents *.bib files containing BibTeX syntax.
 * 
 * @author Matthias Pohl
 */
public class BibtexSource extends AbstractDataSource<BibtexSource> implements Jsonable {

	/**
	 * <code>BibtexSourceIterator</code> is used for generating {@link GenericObject}s out of <code>BibtexSource</code>s.
	 * 
	 * @author Matthias Pohl
	 */
	protected class BibtexSourceIterator extends AbstractDataSourceIterator<BibtexSource> {

		private final Iterator<BibtexAbstractEntry> entryIterator;
		private final boolean retainUnresolvedMacroReferences;

		private final Map<String, BibtexAbstractValue> macros = new HashMap<String, BibtexAbstractValue>();

		// this member should be set to true within convertValue (it's false by default) if the value is a
		// BibtexPersonList with an "isOther" property set
		private boolean otherMode = false;

		/**
		 * Initializes a <code>BibtexSourceIterator</code> using the passed <code>BibtexSource</code>.
		 * 
		 * @param source
		 *            The source of which the data shall be extracted.
		 */
		protected BibtexSourceIterator(BibtexSource source) {
			super(source);
			this.loadMacroDefinitions(this.dataSource.getEntryIterator());

			this.entryIterator = this.dataSource.getEntryIterator();
			this.retainUnresolvedMacroReferences = this.dataSource.retainUnresolvedMacroReferences();

			if (this.entryIterator == null) {
				throw new IllegalStateException("Parsed data is missing.");
			}
		}

		private void loadMacroDefinitions(Iterator<BibtexAbstractEntry> entryIterator) {
			while (entryIterator.hasNext()) {
				final BibtexAbstractEntry currentAbstractEntry = entryIterator.next();

				if (currentAbstractEntry instanceof BibtexMacroDefinition) {
					final BibtexMacroDefinition currentMacro = (BibtexMacroDefinition) currentAbstractEntry;
					if (!BibtexStandardMacros.isStandardMacro(currentMacro.getKey())) {
						this.macros.put(currentMacro.getKey(), currentMacro.getValue());
					}

					BibtexSource.logger.trace("MacroDefinition was read: " + ((BibtexMacroDefinition) currentAbstractEntry).getKey());
				}
			}

		}

		private boolean otherPropertyIsSet() {
			return this.otherMode;
		}

		/**
		 * Takes a {@link BibtexAbstractValue} and returns its Java representation.
		 * 
		 * @param value
		 *            The <code>BibtexAbstractValue</code> that shall be converted.
		 * @return The Java representation of the passed <code>BibtexAbstractValue</code>.
		 */
		private JsonValue convertValue(BibtexAbstractValue value) {

			// initializes other mode (see declaration info of this member)
			// it's possible, that if a person list is passed containing an "other authors" flag -> if this is the case, otherMode will be set to true
			this.otherMode = false;

			if (value instanceof BibtexString) {
				// a normal String
				return new JsonString(((BibtexString) value).getContent());
			} else if (value instanceof BibtexMacroReference) {
				// a macro reference that have to be replaced by its actual macro value

				final BibtexMacroReference ref = (BibtexMacroReference) value;
				if (BibtexStandardMacros.isStandardMacro(ref.getKey())) {
					return new JsonString(BibtexStandardMacros.resolveStandardMacro(ref.getKey()));
				} else if (!this.macros.containsKey(ref.getKey())) {
					if (this.retainUnresolvedMacroReferences) {
						return new JsonString(ref.getKey());
					}

					return null;
				}

				return this.convertValue(this.macros.get(ref.getKey()));
			} else if (value instanceof BibtexPersonList) {
				// a person list have to be converted into an JsonArray
				// this person list can have an additional member for indicating
				// that other authors exist but were not mentioned (@see otherMode
				// member variable)
				final JsonArray personsInJson = new JsonArray();

				final List<BibtexPerson> persons = ((BibtexPersonList) value).getList();

				for (final BibtexPerson person : persons) {
					final JsonRecord personInJson = new JsonRecord();

					// first name
					if (person.getFirst() != null) {
						personInJson.put(BibtexSource.PERSON_FIRST_NAME_ATTRIBUTE, new JsonString(person.getFirst()));
					}

					// prefix of last name
					if (person.getPreLast() != null) {
						personInJson.put(BibtexSource.PERSON_PRE_LAST_NAME_ATTRIBUTE, new JsonString(person.getPreLast()));
					}

					// last name
					if (person.getLast() != null) {
						personInJson.put(BibtexSource.PERSON_LAST_NAME_ATTRIBUTE, new JsonString(person.getLast()));
					}

					// lineage (like 'jr' or 'sr')
					if (person.getLineage() != null) {
						personInJson.put(BibtexSource.PERSON_LINEAGE_ATTRIBUTE, new JsonString(person.getLineage()));
					}

					// otherMode is set to true, if other authors were not
					// explicitly mentioned in the list
					if (person.isOthers()) {
						this.otherMode = true;
					} else {
						personsInJson.add(personInJson);
					}
				}

				if (personsInJson.isEmpty()) {
					return JsonNull.NULL;
				} else if (personsInJson.size() == 1) {
					return personsInJson.get(0);
				}

				return personsInJson;
			} else if (value instanceof BibtexConcatenatedValue) {
				// combines two value concatenated with the hash symbol (#)
				final BibtexConcatenatedValue concatenatedValue = (BibtexConcatenatedValue) value;

				String leftValue = "";
				String rightValue = "";

				if (concatenatedValue.getLeft() != null) {
					leftValue = concatenatedValue.toString();
				}

				if (concatenatedValue.getRight() != null) {
					rightValue = concatenatedValue.toString();
				}

				return new JsonString(leftValue + rightValue);
			} else {
				// should never occur
				throw new IllegalStateException("Unspecified field value was read within the BibTeX file.");
			}
		}

		@Override
		protected JsonRecord loadNextRecord() throws ExtractionFailedException {
			if (!this.entryIterator.hasNext()) {
				return null;
			}

			while (this.entryIterator.hasNext()) {
				final BibtexAbstractEntry currentAbstractEntry = this.entryIterator.next();

				if (currentAbstractEntry instanceof BibtexEntry) {
					final BibtexEntry currentEntry = (BibtexEntry) currentAbstractEntry;

					final JsonRecord retVal = new JsonRecord();

					// the key that is defined for this entry
					this.addAttributeValue(retVal, BibtexSource.KEY_ATTRIBUTE, new JsonString(currentEntry.getEntryKey()));
					// adds the type of the entry (@book, @proceeding, @article, ...; without @)
					this.addAttributeValue(retVal, BibtexSource.TYPE_ATTRIBUTE, new JsonString(currentEntry.getEntryType()));
					// default value for this property is false
					this.setAttributeValue(retVal, BibtexSource.ADDITIONAL_AUTHORS_EXISTS_ATTRIBUTE, JsonBoolean.FALSE);

					final Map<String, BibtexAbstractValue> fields = currentEntry.getFields();
					for (final Map.Entry<String, BibtexAbstractValue> entry : fields.entrySet()) {
						JsonValue convertedValue = this.convertValue(entry.getValue());

						if (convertedValue == null) {
							// null might be returned by convertValue, if entry is a MacroReference that has no corresponding declaration and
							// retaining unresolved macro references is disabled
							continue;
						}

						this.addAttributeValue(retVal, entry.getKey().toLowerCase().intern(), convertedValue);

						if (this.otherPropertyIsSet()) {
							this.setAttributeValue(retVal, BibtexSource.ADDITIONAL_AUTHORS_EXISTS_ATTRIBUTE, JsonBoolean.TRUE);
						}
					}

					return retVal;
				} else if (currentAbstractEntry instanceof BibtexMacroDefinition) {
					BibtexSource.logger.trace("MacroDefinition was read: " + ((BibtexMacroDefinition) currentAbstractEntry).getKey());
				} else if (currentAbstractEntry instanceof BibtexPreamble) {
					BibtexSource.logger.trace("Preamble was read: " + ((BibtexPreamble) currentAbstractEntry).getContent());
				} else if (currentAbstractEntry instanceof BibtexToplevelComment) {
					BibtexSource.logger.trace("ToplevelComment was read: " + ((BibtexToplevelComment) currentAbstractEntry).getContent().trim());
				} else {
					// should never occur
					throw new IllegalStateException("Unspecified entry was read within the BibTeX file.");
				}
			}

			return null;
		}

	}

	private static final Logger logger = Logger.getLogger(BibtexSource.class.getPackage().getName());

	/**
	 * The name of the attribute that stores the key.
	 */
	public static final String KEY_ATTRIBUTE = "bibtexkey";

	/**
	 * The name of the attribute that stores the type.
	 */
	public static final String TYPE_ATTRIBUTE = "type";

	/**
	 * The name of the attribute for storing information, whether there is an explicit mention that not all authors are listed. ('others' keyword or
	 * something similar to that within the authors list)
	 */
	public static final String ADDITIONAL_AUTHORS_EXISTS_ATTRIBUTE = "otherauthors";

	/**
	 * The name of the attribute that stores the first name of a person.
	 */
	public static final String PERSON_FIRST_NAME_ATTRIBUTE = "firstname";

	/**
	 * The name of the attribute that stores the prefix of the person's last name.
	 */
	public static final String PERSON_PRE_LAST_NAME_ATTRIBUTE = "prelastname";

	/**
	 * The name of the attribute that stores the last name of a person.
	 */
	public static final String PERSON_LAST_NAME_ATTRIBUTE = "lastname";

	/**
	 * The name of the attribute that stores the lineage (like 'jr' or 'sr') of a person.
	 */
	public static final String PERSON_LINEAGE_ATTRIBUTE = "lineage";

	private transient BibtexFile bibtexStructure;
	private transient File bibtexFile;
	private boolean resolveCrossReferences;
	private boolean retainUnresolvedMacros = false;

	/**
	 * Internal constructor for {@link Jsonable} deserialization.
	 */
	protected BibtexSource() {
		super();
		// nothing to do
	}

	/**
	 * Initializes a <code>BibtexSource</code>.
	 * 
	 * @param identifier
	 *            The identifier of the {@link DataSource} instance.
	 * @param file
	 *            The file of which the data will be extracted.
	 * @throws FileNotFoundException
	 *             If the passed file was not found on the file system.
	 * @throws IOException
	 *             If an error occurs while reading the file.
	 * @throws ExpansionException
	 *             If an error occurred while resolving cross references, macros, etc.
	 * @throws ParseException
	 *             If the BibTeX syntax of the passed file is invalid.
	 */
	public BibtexSource(String identifier, File file) throws FileNotFoundException, IOException, ExpansionException, ParseException {
		this(identifier, file, true);
	}

	/**
	 * Initializes a <code>BibtexSource</code> object.
	 * 
	 * @param identifier
	 *            The identifier of the {@link DataSource} instance.
	 * @param file
	 *            The <code>File</code> object, from which the data will be extracted.
	 * @param resolveCrossReferences
	 *            Determines whether cross references shall be resolved or not.
	 * @throws FileNotFoundException
	 *             If the passed file was not found on the file system.
	 * @throws IOException
	 *             If an error occurs while reading the file.
	 * @throws ExpansionException
	 *             If an error occurred while resolving cross references, macros, etc.
	 * @throws ParseException
	 *             If the BibTeX syntax of the passed file is invalid.
	 */
	public BibtexSource(String identifier, File file, boolean resolveCrossReferences) throws FileNotFoundException, IOException,
			ExpansionException, ParseException {
		super(identifier);

		if (file == null) {
			throw new NullPointerException("Bibtex file is missing.");
		} else if (!file.exists()) {
			throw new FileNotFoundException("The passed file was not found on the file system.");
		}

		this.bibtexFile = file;

		this.resolveCrossReferences = resolveCrossReferences;

		this.parseFile();
	}
	
	private void parseFile() throws ExpansionException, IOException, ParseException {
		final BibtexParser parser = new BibtexParser(false);

		this.bibtexStructure = new BibtexFile();

		final Reader reader = new InputStreamReader(new FileInputStream(this.bibtexFile), GlobalConfig.getInstance().getDefaultEncoding());
		parser.parse(this.bibtexStructure, reader);
		reader.close();

		// expand macros, cross references and the names of authors and editors
		new MacroReferenceExpander(true, true, false, false).expand(this.bibtexStructure);
		if (this.resolveCrossReferences) {
			new CrossReferenceExpander(false).expand(this.bibtexStructure);
		}
		new PersonListExpander(true, false, false).expand(this.bibtexStructure);
	}

	/**
	 * Enables retaining the macros, if their value cannot be resolved. This option is disabled by default.
	 */
	public void enableRetainingUnresolvedMacroReferences() {
		this.retainUnresolvedMacros = true;
	}

	/**
	 * Disables retaining the macros, if their value cannot be resolved. This option is disabled by default.
	 */
	public void disableRetainingUnresolvedMacroReferences() {
		this.retainUnresolvedMacros = false;
	}

	/**
	 * Checks, whether unresolved macro references are retained. This option is disabled by default.
	 * 
	 * @return <code>true</code>, if unresolved macro references will be retained; otherwise <code>false</code>.
	 */
	public boolean retainUnresolvedMacroReferences() {
		return this.retainUnresolvedMacros;
	}

	/**
	 * Returns the entry iterator.
	 * 
	 * @return An iterator returning all entries of the parsed file.
	 */
	protected Iterator<BibtexAbstractEntry> getEntryIterator() {
		if (this.bibtexStructure == null) {
			return null;
		}

		return this.bibtexStructure.getEntries().iterator();
	}

	@Override
	public void close() {
		// nothing to do - closing the connection to the underlying file is already done in the constructor
	}

	@Override
	public Iterator<GenericObject> iterator() {
		return new BibtexSourceIterator(this);
	}

	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException {
		jsonGenerator.writeRecordStart();
		JsonUtil.writeFields(jsonGenerator, this);
		jsonGenerator.writeRecordEntry("file", new JsonString(this.bibtexFile.getCanonicalPath()));
		jsonGenerator.writeRecordEnd();
	}

	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException {
		jsonParser.skipToken(JsonToken.START_OBJECT);
		JsonUtil.readFields(jsonParser, this);
		jsonParser.skipFieldName("file");
		this.bibtexFile = new File(jsonParser.nextString());
		jsonParser.skipToken(JsonToken.END_OBJECT);
		
		try {
			this.parseFile();
		} catch (ExpansionException e) {
			throw new IllegalStateException("The passed file could not be parsed.", e);
		} catch (ParseException e) {
			throw new IllegalStateException("The passed file could not be parsed.", e);
		}
	}

}
