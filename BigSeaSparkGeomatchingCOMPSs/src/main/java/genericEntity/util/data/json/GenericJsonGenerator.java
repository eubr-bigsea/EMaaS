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

package genericEntity.util.data.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import genericEntity.util.data.AutoJsonable;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.auto.JsonTypeManager;

/**
 * <code>GenericJsonGenerator</code> is another implementation for generating Json code. It is used by {@link Jsonable#toJson(GenericJsonGenerator)}. This
 * approach is faster than the old implementation {Jsonable#toJsonString(int)} but slower than {Jsonable#toJsonString()}.
 * 
 * @author Matthias Pohl
 */
public class GenericJsonGenerator implements Closeable {

	private static JsonFactory factory = new JsonFactory();

	private JsonGenerator generator;

	/**
	 * Initializes a new <code>GenericJsonGenerator</code>.
	 * 
	 * @param outStream
	 *            The stream that is used for the output.
	 * @throws IOException
	 *             If an error occurs while accessing the <code>writer</code>.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a {@link OutputStream}.
	 */
	public GenericJsonGenerator(OutputStream outStream) throws IOException {
		this(new OutputStreamWriter(outStream));
	}

	/**
	 * Initializes a new <code>GenericJsonGenerator</code>.
	 * 
	 * @param writer
	 *            The writer that is used internally.
	 * @throws IOException
	 *             If an error occurs while accessing the <code>writer</code>.
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a {@link Writer}.
	 */
	public GenericJsonGenerator(Writer writer) throws IOException {
		if (writer == null) {
			throw new NullPointerException("The writer is missing.");
		}

		this.generator = GenericJsonGenerator.factory.createJsonGenerator(writer);
	}

	/**
	 * Initializes a new <code>GenericJsonGenerator</code> with the given generator.
	 * 
	 * @param generator
	 *            The generator to wrap
	 * @throws NullPointerException
	 *             If <code>null</code> was passed instead of a {@link JsonGenerator}.
	 */
	protected GenericJsonGenerator(JsonGenerator generator) {
		if (generator == null)
			throw new NullPointerException();
		this.generator = generator;
	}

	/**
	 * Flushes the internal buffer.
	 * 
	 * @throws IOException
	 *             If an error occurs while flushing the data.
	 */
	public void flush() throws IOException {
		this.generator.flush();
	}

	/**
	 * Enables formatted Json. Calling this method will induce the generator to create formatted Json code. This makes the code easier to read for
	 * humans but increases the runtime.
	 */
	public void enableFormattedJson() {
		this.generator.useDefaultPrettyPrinter();
	}

	/**
	 * Disables formatted Json. Calling this method will induce the generator to create unformatted Json code. This makes the code harder to read for
	 * humans but decreases the runtime.
	 */
	public void disableFormattedJson() {
		this.generator.setPrettyPrinter(null);
	}

	/**
	 * Writes the passed {@link JsonValue}.
	 * 
	 * @param value
	 *            The <code>JsonValue</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonValue(JsonValue value) throws JsonGenerationException, IOException {
		if (value == null) {
			this.writeJsonRecord(new JsonRecord());
			return;
		}

		switch (value.getType()) {
		case Array:
			this.writeJsonArray((JsonArray) value);
			return;
		case Boolean:
			this.writeJsonBoolean((JsonBoolean) value);
			return;
		case Null:
			this.writeJsonNull();
			return;
		case Number:
			this.writeJsonNumber((JsonNumber) value);
			return;
		case Record:
			this.writeJsonRecord((JsonRecord) value);
			return;
		case String:
			this.writeJsonString((JsonString) value);
			return;
		default:
			throw new JsonGenerationException("Unknown Json type: " + value.getType());
		}
	}

	/**
	 * Writes the passed {@link JsonArray}.
	 * 
	 * @param array
	 *            The <code>JsonArray</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonArray(JsonArray array) throws JsonGenerationException, IOException {
		this.generator.writeStartArray();
		for (JsonValue value : array) {
			this.writeJsonValue(value);
		}
		this.generator.writeEndArray();
		this.generator.flush();
	}

	/**
	 * Writes the passed {@link JsonBoolean}.
	 * 
	 * @param value
	 *            The <code>JsonBoolean</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonBoolean(JsonBoolean value) throws JsonGenerationException, IOException {
		this.generator.writeBoolean(value.getValue());
		this.generator.flush();
	}

	/**
	 * Writes a {@link JsonNull} value to the stream.
	 * 
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonNull() throws JsonGenerationException, IOException {
		this.generator.writeNull();
		this.generator.flush();
	}

	/**
	 * Writes the passed {@link JsonNumber}.
	 * 
	 * @param value
	 *            The <code>JsonNumber</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonNumber(JsonNumber value) throws JsonGenerationException, IOException {
		this.generator.writeNumber(value.getValue().toString());
		this.generator.flush();
	}

	/**
	 * Writes the passed {@link JsonRecord}.
	 * 
	 * @param record
	 *            The <code>JsonRecord</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonRecord(JsonRecord record) throws JsonGenerationException, IOException {
		this.writeRecordStart();
		for (Map.Entry<String, JsonValue> entry : record.entrySet()) {
			this.writeRecordEntry(entry.getKey(), entry.getValue());
		}
		this.writeRecordEnd();
		this.generator.flush();
	}

	/**
	 * Writes the passed {@link JsonString}.
	 * 
	 * @param value
	 *            The <code>JsonString</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeJsonString(JsonString value) throws JsonGenerationException, IOException {
		this.generator.writeString(value.toString());
	}

	/**
	 * Writes a record start into the stream.
	 * 
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeRecordStart() throws JsonGenerationException, IOException {
		this.generator.writeStartObject();
	}

	/**
	 * Writes a record end into the stream.
	 * 
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeRecordEnd() throws JsonGenerationException, IOException {
		this.generator.writeEndObject();
		this.generator.flush();
	}

	/**
	 * Writes an array start into the stream.
	 * 
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeArrayStart() throws JsonGenerationException, IOException {
		this.generator.writeStartArray();
	}

	/**
	 * Writes an array end into the stream.
	 * 
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeArrayEnd() throws JsonGenerationException, IOException {
		this.generator.writeEndArray();
		this.generator.flush();
	}

	/**
	 * Writes a new record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param value
	 *            The <code>JsonValue</code> that shall be converted into Json code.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeRecordEntry(String fieldname, JsonValue value) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		this.writeJsonValue(value);
	}

	/**
	 * Writes a new field name into the stream.
	 * 
	 * @param fieldname
	 *            The field name.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeRecordFieldName(String fieldname) throws JsonGenerationException, IOException {
		this.generator.writeFieldName(fieldname);
	}

	public void close() throws IOException {
		this.generator.close();
	}

	/**
	 * Writes a raw string into the stream.
	 * 
	 * @param string
	 *            The string to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeString(String string) throws JsonGenerationException, IOException {
		this.generator.writeString(string);
	}

	/**
	 * Writes a raw string into the stream.
	 * 
	 * @param number
	 *            The number to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeNumber(Number number) throws JsonGenerationException, IOException {
		this.generator.writeNumber(number.toString());
	}

	/**
	 * Writes a raw boolean into the stream.
	 * 
	 * @param value
	 *            The value to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeBoolean(boolean value) throws JsonGenerationException, IOException {
		this.generator.writeBoolean(value);
	}

	/**
	 * Writes a new string record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param value
	 *            The string to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeStringRecordEntry(String fieldname, String value) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		this.writeString(value);
	}

	/**
	 * Writes a new number record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param value
	 *            The number to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeNumberRecordEntry(String fieldname, Number value) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		this.writeNumber(value);
	}

	/**
	 * Writes a new boolean record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param value
	 *            The boolean to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeBooleanRecordEntry(String fieldname, boolean value) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		this.writeBoolean(value);
	}

	/**
	 * Writes a new collection record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param jsonables
	 *            The collection of {@link Jsonable}s to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeArrayRecordEntry(String fieldname, Collection<? extends Jsonable> jsonables) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		this.writeArrayStart();
		for (Jsonable jsonable : jsonables)
			jsonable.toJson(this);
		this.writeArrayEnd();
	}

	/**
	 * Writes a new record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param jsonable
	 *            The {@link Jsonable} to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeRecordEntry(String fieldname, Jsonable jsonable) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		this.writeRecordStart();
		jsonable.toJson(this);
		this.writeRecordEnd();
	}

	/**
	 * Writes a new record entry into the stream
	 * 
	 * @param fieldname
	 *            The field name of the entry.
	 * @param jsonable
	 *            The {@link Jsonable} to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	public void writeRecordEntry(String fieldname, AutoJsonable jsonable) throws JsonGenerationException, IOException {
		this.writeRecordFieldName(fieldname);
		writeRecord(jsonable);
	}

	/**
	 * Writes a new record entry into the stream
	 * 
	 * @param jsonable
	 *            The {@link Jsonable} to write
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while accessing the underlying stream.
	 */
	@SuppressWarnings("unchecked")
	public void writeRecord(AutoJsonable jsonable) throws JsonGenerationException, IOException {
		if (jsonable == null)
			this.writeJsonNull();
		else if (jsonable instanceof Jsonable)
			((Jsonable) jsonable).toJson(this);
		else
			JsonTypeManager.getInstance().getTypeInfo((Class<AutoJsonable>) jsonable.getClass()).write(this, jsonable);
	}

}
