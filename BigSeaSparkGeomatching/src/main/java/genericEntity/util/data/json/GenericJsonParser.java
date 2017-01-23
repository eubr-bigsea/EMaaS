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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.util.JsonParserSequence;
import org.codehaus.jackson.util.TokenBuffer;

import genericEntity.exception.InvalidJsonException;
import genericEntity.util.AbstractIterator;
import genericEntity.util.ReflectUtil;
import genericEntity.util.data.AutoJsonable;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.auto.JsonTypeManager;

/**
 * <code>GenericJsonParser</code> can be used for converting a String containing Json syntax into its Java representation. Besides providing methods for
 * reading the data it also implements the {@link Iterator} interface. The iterating functionality can be used for parsing the content of a
 * {@link JsonArray}. <code>GenericJsonParser</code> can be used for converting a Json String into its Java representation. Besides providing methods for
 * reading the data it also implements the {@link Iterator} interface. The iterating functionality can be used for parsing the content of a
 * {@link JsonArray}.
 * 
 * @author Matthias Pohl
 * @param <T>
 *            The type that is used for instantiating during the iteration process.
 */
public class GenericJsonParser<T extends Jsonable> extends AbstractIterator<T> implements Closeable {

	private static final Logger logger = Logger.getLogger(GenericJsonParser.class.getPackage().getName());

	private static JsonFactory factory = new JsonFactory();

	/**
	 * Converts the passed String into the corresponding atomic {@link JsonValue}.
	 * 
	 * @param jsonCode
	 *            The String that will be converted.
	 * @return The corresponding <code>JsonValue</code>.
	 */
	public static JsonValue getScalarValue(String jsonCode) {
		if (jsonCode.equalsIgnoreCase("true"))
			return JsonBoolean.TRUE;
		else if (jsonCode.equalsIgnoreCase("false"))
			return JsonBoolean.FALSE;
		else if (jsonCode.equalsIgnoreCase("null"))
			return JsonNull.NULL;

		try {
			final JsonParser parser = GenericJsonParser.factory.createJsonParser(jsonCode);
			parser.nextToken();
			if (parser.getCurrentToken().isNumeric())
				return new JsonNumber(parser.getNumberValue());
			else if (!parser.hasCurrentToken())
				return new JsonString();

			final String text = parser.getText();
			parser.close();

			return new JsonString(text);
		} catch (final IOException e) {
			GenericJsonParser.logger.warn("Scalar value could not be converted into a JsonValue: " + jsonCode, e);
			return new JsonString(jsonCode);
		}
	}

	private JsonParser parser;

	private JsonToken currentToken;
	// array iteration functionality
	private boolean arrayStarted = false;

	private boolean arrayFinished = false;

	/**
	 * is needed for creating new instances (@see Jsonable#fromJson(GenericJsonParser))
	 */
	protected Class<T> internalType;

	private JsonParser originalParser, pushBackParser;

	private TokenBuffer pushBackGenerator;

	/**
	 * Internal constructor for sub classing.
	 * 
	 * @param type
	 *            The type that is generated during the iteration process. {@link JsonValue} instances are returned, if <code>null</code> is passed.
	 * 
	 */
	protected GenericJsonParser(Class<T> type) {
		this.internalType = type;
	}

	/**
	 * Initializes a new <code>GenericJsonParser</code> using the passed {@link InputStream}.
	 * 
	 * @param type
	 *            The type that is generated during the iteration process. {@link JsonValue} instances are returned, if <code>null</code> is passed.
	 * @param stream
	 *            The stream whose data will be parsed.
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	public GenericJsonParser(Class<T> type, InputStream stream) throws JsonParseException, IOException {
		this.parser = GenericJsonParser.factory.createJsonParser(stream);
		this.internalType = type;
		this.loadNextToken();
	}

	/**
	 * Initializes a new <code>GenericJsonParser</code> using the passed {@link Reader}.
	 * 
	 * @param type
	 *            The type that is generated during the iteration process. {@link JsonValue} instances are returned, if <code>null</code> is passed.
	 * @param reader
	 *            The reader that is used for parsing the data.
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	public GenericJsonParser(Class<T> type, Reader reader) throws JsonParseException, IOException {
		this.parser = GenericJsonParser.factory.createJsonParser(reader);
		this.internalType = type;
		this.loadNextToken();
	}

	/**
	 * Initializes a new <code>GenericJsonParser</code> that parses the passed <code>String</code>.
	 * 
	 * @param type
	 *            The type that is generated during the iteration process. {@link JsonValue} instances are returned, if <code>null</code> is passed.
	 * @param str
	 *            The code that shall be parsed.
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	public GenericJsonParser(Class<T> type, String str) throws JsonParseException, IOException {
		this(type, new StringReader(str));
	}

	/**
	 * Initializes a new <code>GenericJsonParser</code> using the passed {@link InputStream}.
	 * 
	 * @param stream
	 *            The stream whose data will be parsed.
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	public GenericJsonParser(InputStream stream) throws JsonParseException, IOException {
		this(null, stream);
	}

	/**
	 * Initializes a new <code>GenericJsonParser</code> using the passed {@link Reader}.
	 * 
	 * @param reader
	 *            The reader that is used for parsing the data.
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	public GenericJsonParser(Reader reader) throws JsonParseException, IOException {
		this(null, reader);
	}

	/**
	 * Initializes a new <code>GenericJsonParser</code> that parses the passed <code>String</code>.
	 * 
	 * @param str
	 *            The code that shall be parsed.
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	public GenericJsonParser(String str) throws JsonParseException, IOException {
		this(null, new ByteArrayInputStream(str.getBytes()));
	}

	/**
	 * Closes the used reader.
	 * 
	 * @see Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		if (!this.parser.isClosed())
			this.parser.close();

		this.currentToken = null;
	}

	/**
	 * Cleans up helper structures used with {@link #createPushBackGenerator()}.<br>
	 * This method should especially be invoked when all pushed json elements have been read.<br>
	 * Without calling this method, a slight performance decrease should be observable. However, the parser still works correctly.
	 */
	public void consolidatePushBack() {
		if (this.parser instanceof JsonParserSequence && !this.isPushBackActive()) {
			this.parser = this.originalParser;
			this.pushBackParser = null;
			this.pushBackGenerator = null;
		}
	}

	private T createInstance() throws IllegalArgumentException {
		return ReflectUtil.newInstance(this.internalType);
	}

	/**
	 * Creates a new <code>JsonParseException</code>.
	 * 
	 * @param msg
	 *            The error message.
	 * @return A newly created <code>JsonParseException</code> instance.
	 */
	protected JsonParseException createJsonParseException(String msg) {
		return this.createJsonParseException(msg, null);
	}

	/**
	 * Creates a new <code>JsonParseException</code>.
	 * 
	 * @param msg
	 *            The error message.
	 * @param cause
	 *            The cause of this exception.
	 * @return A newly created <code>JsonParseException</code> instance.
	 */
	protected JsonParseException createJsonParseException(String msg, Throwable cause) {
		return new JsonParseException(msg, this.parser.getTokenLocation(), cause);
	}

	/**
	 * Creates a push back generator for the parser similar to {@link PushbackInputStream}.<br>
	 * The returned {@link GenericJsonGenerator} might be used to generate arbitrary json elements in front of the current parser position in a FIFO
	 * manner.<br>
	 * In order to adjust the internal state to the newly generated json elements, a closing the returned generator with
	 * {@link GenericJsonGenerator#close()} is necessary.
	 * 
	 * @return the push back generator
	 */
	public GenericJsonGenerator createPushBackGenerator() {
		if (this.originalParser == null)
			this.originalParser = this.parser;
		final boolean oldPushBackActive = this.isPushBackActive();
		final JsonParser currentParser = oldPushBackActive ? this.parser : this.originalParser;
		this.pushBackGenerator = new TokenBuffer(null);
		this.parser = JsonParserSequence.createFlattened(this.pushBackParser = this.pushBackGenerator.asParser(), currentParser);
		return new GenericJsonGenerator(this.pushBackGenerator) {
			@Override
			public void close() throws IOException {
				super.close();
				GenericJsonParser.this.loadNextToken();
			}
		};
	}

	/**
	 * Returns the current field name without setting the cursor to the next token.
	 * 
	 * @return The current field name.
	 * @throws JsonParseException
	 *             If an error occurs while parsing the Json.
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 */
	public String currentFieldName() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.FIELD_NAME)
			throw this.createJsonParseException("nextFieldName: field name expected but was " + this.currentTokenName() + ".");

		return this.parser.getText();
	}

	/**
	 * Returns the current token that was loaded during the last {@link #nextToken()} call.
	 * 
	 * @return The current token.
	 * @throws EOFException
	 *             If the end of the content was already reached.
	 */
	public JsonToken currentToken() throws EOFException {
		if (this.endOfContentReached())
			throw new EOFException("End of Json content reached.");

		return this.currentToken;
	}

	/**
	 * Returns the type name of the current token.
	 * 
	 * @return The type name of the current token.
	 */
	protected String currentTokenName() {
		if (!this.hasCurrentToken())
			return "no token";

		try {
			return this.currentToken().name();
		} catch (final EOFException e) {
			GenericJsonParser.logger.warn("EOF: currentTokenName was called althoug the end of the content was reached.", e);
			return null;
		}
	}

	/**
	 * Checks whether the end of the content was reached.
	 * 
	 * @return <code>true</code>, if {@link #readNextCharacter()} will return <code>null</code>; otherwise <code>false</code>.
	 */
	public boolean endOfContentReached() {
		return !this.hasCurrentToken();
	}

	/**
	 * Checks whether the current token declares the end of a complex Json structure.
	 * 
	 * @return <code>true</code>, if the end of an array or a record is reached; otherwise <code>false</code>.
	 * @throws EOFException
	 *             If the stream's end was reached.
	 */
	public boolean endTokenReached() throws EOFException {
		return this.currentToken() == JsonToken.END_ARRAY || this.currentToken() == JsonToken.END_OBJECT;
	}

	/**
	 * Closes all streams if it is not already done.
	 */
	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}

	/**
	 * Checks whether a token is loaded.
	 * 
	 * @return <code>true</code>, if {@link #currentToken()} would return the current token; <code>false</code>, if the end of the content was already
	 *         reached.
	 */
	protected boolean hasCurrentToken() {
		return this.currentToken != null;
	}

	private boolean isPushBackActive() {
		return this.parser instanceof JsonParserSequence && this.pushBackParser != null && this.pushBackParser.hasCurrentToken();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T loadNextElement() {
		try {
			if (this.arrayFinished)
				return null;

			if (!this.arrayStarted)
				if (this.currentToken() == JsonToken.START_ARRAY) {
					this.arrayStarted = true;
					this.nextToken(); // skip '['
				} else
					// throw new
					// InvalidJsonException("next(): '[' expected, but was '" +
					// this.currentTokenName() + "'.");
					// only one element
					this.arrayFinished = true;

			// end of array
			if (this.currentToken() == JsonToken.END_ARRAY) {
				this.arrayFinished = true;
				return null;
			}

			if (this.internalType == null)
				return (T) this.nextJsonValue();

			final T returnValue = this.createInstance();
			returnValue.fromJson(this);

			return returnValue;
		} catch (final JsonParseException e) {
			throw new InvalidJsonException("next(): invalid Json format.", e);
		} catch (final IOException e) {
			throw new InvalidJsonException("Error occurred during reading process...", e);
		} catch (final IllegalArgumentException e) {
			throw new InvalidJsonException("Error occurred while creating a new instance...", e);
		}
	}

	private void loadNextToken() throws JsonParseException, IOException {
		this.currentToken = this.parser.nextToken();
	}

	/**
	 * Returns the current boolean value.
	 * 
	 * @return The current boolean value.
	 * @throws JsonParseException
	 *             If an error occurs while parsing the Json.
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 */
	public boolean nextBoolean() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.VALUE_FALSE && this.currentToken() != JsonToken.VALUE_TRUE)
			throw this.createJsonParseException("nextString: boolean expected but was " + this.currentTokenName() + ".");

		final boolean returnValue = this.parser.getBooleanValue();
		this.nextToken(); // skip String
		return returnValue;
	}

	/**
	 * Returns the current field name.
	 * 
	 * @return The current field name.
	 * @throws JsonParseException
	 *             If an error occurs while parsing the Json.
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 */
	public String nextFieldName() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.FIELD_NAME)
			throw this.createJsonParseException("nextFieldName: field name expected but was " + this.currentTokenName() + ".");

		if (this.currentToken() == JsonToken.FIELD_NAME) {
			final String returnValue = this.parser.getText();
			this.nextToken(); // skip String

			return returnValue;
		}

		throw this.createJsonParseException("nextJsonString: String value expected but was " + this.currentTokenName() + ".");
	}

	/**
	 * Returns the next Json instance. This instance is casted into the passed type.
	 * 
	 * @param <ReturnType>
	 *            The type that shall be returned.
	 * @param returnType
	 *            The type that shall be returned.
	 * @return The parsed instance.
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading the data.
	 * @throws InstantiationException
	 *             If an error occurred while instantiating the new object.
	 * @throws IllegalAccessException
	 *             If an access error occurred while instantiating the new object.
	 */
	public <ReturnType extends Jsonable> ReturnType nextJsonable(Class<ReturnType> returnType) throws JsonParseException, IOException {
		final ReturnType returnValue = ReflectUtil.newInstance(returnType);
		returnValue.fromJson(this);
		return returnValue;
	}

	/**
	 * Returns the next {@link JsonArray}. The syntax of the <code>JsonArray</code> has to start directly. Any leading whitespaces or similar
	 * characters will cause a ParseException.
	 * 
	 * @return The next <code>JsonArray</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonArray</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonArray nextJsonArray() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.START_ARRAY)
			throw this.createJsonParseException("nextJsonArray: '[' expected but was " + this.currentToken().name() + ".");

		this.nextToken(); // skip '['

		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");

		final JsonArray returnValue = new JsonArray();
		while (this.currentToken() != JsonToken.END_ARRAY)
			returnValue.add(this.nextJsonValue());

		this.nextToken(); // skip ']'

		return returnValue;
	}

	/**
	 * Returns the next {@link JsonBoolean}. The syntax of the <code>JsonBoolean</code> has to start directly. Any leading whitespaces or similar
	 * characters will cause a ParseException.
	 * 
	 * @return The next <code>JsonBoolean</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonBoolean</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonBoolean nextJsonBoolean() throws JsonParseException, IOException {
		JsonBoolean returnValue = null;

		if (this.currentToken() == JsonToken.VALUE_TRUE)
			returnValue = JsonBoolean.TRUE;
		else if (this.currentToken() == JsonToken.VALUE_FALSE)
			returnValue = JsonBoolean.FALSE;
		else if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else
			throw this.createJsonParseException("nextJsonBoolean: boolean value expected but was " + this.currentToken().name() + ".");

		this.nextToken(); // skip "true" or "false"

		return returnValue;
	}

	/**
	 * Returns the next <code>JsonNull</code>. The syntax of the <code>JsonNull</code> has to start directly. Any leading whitespaces or similar
	 * characters will cause a ParseException.
	 * 
	 * @return The next <code>JsonNull</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonNull</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonNull nextJsonNull() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.VALUE_NULL)
			throw this.createJsonParseException("nextJsonNull: 'null' expected but was " + this.currentTokenName() + ".");

		this.nextToken(); // skip "null"

		return JsonNull.NULL;
	}

	/**
	 * Returns the next {@link JsonNumber}. The syntax of the <code>JsonNumber</code> has to start directly. Any leading whitespaces or similar
	 * characters will cause a ParseException. The passed number will be converted into one of the following types: Integer, Long, or BigDecimal;
	 * Float or Double. The "smallest" data type will be preferred (e.g.: 10000 is converted into a JsonNumber<Integer> type since it fits into the
	 * Integer range.).
	 * 
	 * @return The next <code>JsonNumber</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonNumber</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonNumber nextJsonNumber() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (!this.currentToken().isNumeric())
			throw this.createJsonParseException("nextJsonNumber: numeric value expected but was " + this.currentToken().name() + ".");

		JsonNumber returnValue = new JsonNumber(this.parser.getNumberValue());

		// this check needs to be done in order to support BigDecimal (otherwise
		// Double.Infinite would be used for really large floating numbers)
		if (Double.isInfinite(this.parser.getDoubleValue()))
			returnValue = new JsonNumber(this.parser.getDecimalValue());

		this.nextToken(); // skip number

		return returnValue;
	}

	/**
	 * Returns the next {@link JsonRecord}. The syntax of the <code>JsonRecord</code> has to start directly. Any leading whitespaces or similar
	 * characters will cause a ParseException.
	 * 
	 * @return The next <code>JsonRecord</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonRecord</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonRecord nextJsonRecord() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.START_OBJECT)
			throw this.createJsonParseException("nextJsonRecord: '{' expected but was " + this.currentTokenName() + ".");

		this.nextToken(); // skip '{'

		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");

		final JsonRecord returnValue = new JsonRecord();
		while (this.currentToken() != JsonToken.END_OBJECT) {
			if (!this.hasCurrentToken())
				// end of stream reached
				throw new EOFException("End of content reached");
			else if (this.currentToken() != JsonToken.FIELD_NAME)
				throw this.createJsonParseException("nextJsonRecord: '{' expected but was " + this.currentTokenName() + ".");

			final String attributeName = this.parser.getCurrentName();
			this.nextToken(); // skip field name
			returnValue.put(attributeName, this.nextJsonValue());
		}

		this.nextToken(); // skip '}'

		return returnValue;
	}

	/**
	 * Returns the next {@link JsonString}. The syntax of the <code>JsonString</code> has to start directly. Any leading whitespaces or similar
	 * characters will cause a JsonParseException.
	 * 
	 * @return The next <code>JsonString</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonString</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonString nextJsonString() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else if (this.currentToken() != JsonToken.VALUE_STRING)
			throw this.createJsonParseException("nextJsonString: String value expected but was " + this.currentTokenName() + ".");

		if (this.currentToken() == JsonToken.VALUE_STRING) {
			final JsonString returnValue = new JsonString(this.parser.getText());
			this.nextToken(); // skip String

			return returnValue;
		}

		throw this.createJsonParseException("nextJsonString: String value expected but was " + this.currentTokenName() + ".");
	}

	/**
	 * Returns any {@link JsonValue}. The syntax of the <code>JsonValue</code> has to start directly. Any leading whitespaces or similar characters
	 * will cause a ParseException.
	 * 
	 * @return The next <code>JsonValue</code>.
	 * @throws JsonParseException
	 *             If the read <code>JsonValue</code> is invalid in terms of the Json syntax or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	public JsonValue nextJsonValue() throws JsonParseException, IOException {
		JsonValue returnValue = null;
		if (this.currentToken() == JsonToken.VALUE_STRING)
			returnValue = this.nextJsonString();
		else if (this.currentToken() == JsonToken.START_ARRAY)
			returnValue = this.nextJsonArray();
		else if (this.currentToken() == JsonToken.START_OBJECT)
			returnValue = this.nextJsonRecord();
		else if (this.currentToken().isNumeric())
			returnValue = this.nextJsonNumber();
		else if (this.currentToken() == JsonToken.VALUE_TRUE || this.currentToken() == JsonToken.VALUE_FALSE)
			returnValue = this.nextJsonBoolean();
		else if (this.currentToken() == JsonToken.VALUE_NULL)
			returnValue = this.nextJsonNull();
		else if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		else
			throw this.createJsonParseException("Unknown Json token: " + this.currentTokenName());

		return returnValue;
	}

	/**
	 * Returns the next instance of the given type initialized with the current json.
	 * 
	 * @param clazz
	 *            the type whose instance is being created
	 * @param <C>
	 *            the type whose instance is being created
	 * 
	 * @return an instance of clazz or null if the current json value is null.
	 * @throws JsonParseException
	 *             If the read json is invalid or some leading whitespaces are read.
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 */
	@SuppressWarnings("unchecked")
	public <C extends AutoJsonable> C nextObject(Class<C> clazz) throws JsonParseException, IOException {
		if (this.currentToken() == JsonToken.VALUE_NULL)
			return null;
		if (Jsonable.class.isAssignableFrom(clazz))
			return (C) nextJsonable((Class) clazz);
		return JsonTypeManager.getInstance().getTypeInfo(clazz).read(this);
	}

	/**
	 * Returns the current textual value.
	 * 
	 * @return The current textual value.
	 * @throws JsonParseException
	 *             If an error occurs while parsing the Json.
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 */
	public String nextString() throws JsonParseException, IOException {
		if (!this.hasCurrentToken())
			// end of stream reached
			throw new EOFException("End of content reached");
		if (this.currentToken() == JsonToken.VALUE_NULL) {
			this.nextToken(); // skip null
			return null;
		}
		if (this.currentToken() != JsonToken.VALUE_STRING)
			throw this.createJsonParseException("nextString: string expected but was " + this.currentTokenName() + ".");

		final String returnValue = this.parser.getText();
		this.nextToken(); // skip String
		return returnValue;
	}

	/**
	 * Loads the next token from the stream.
	 * 
	 * @throws JsonParseException
	 *             If an error occurred while parsing the data.
	 * @throws IOException
	 *             If an error occurred while reading from the stream.
	 */
	protected void nextToken() throws JsonParseException, IOException {
		if (this.endOfContentReached())
			return;

		this.loadNextToken();

		if (!this.hasCurrentToken())
			this.close();
	}

	/**
	 * Safely skips the current token if it is a field name with the specified value.
	 * 
	 * @param fieldName
	 *            The expected field name
	 * 
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 * @throws JsonParseException
	 *             If the current token is not the expected field name or an error occurs while parsing the Json.
	 */
	public void skipFieldName(String fieldName) throws JsonParseException, IOException {
		if (this.currentToken() != JsonToken.FIELD_NAME)
			throw new JsonParseException("expected field name " + fieldName + "; found " + this.currentToken(), null);
		final String actualFieldName = this.nextFieldName();
		if (!actualFieldName.equals(fieldName))
			throw new JsonParseException("expected field name " + fieldName + "; found " + actualFieldName, null);
	}

	/**
	 * Skips the current token.
	 * 
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 * @throws JsonParseException
	 *             If an error occurs while parsing the Json.
	 */
	public void skipToken() throws JsonParseException, IOException {
		this.nextToken();
	}

	/**
	 * Safely skips the current token if it is of the given type.
	 * 
	 * @param expectedType
	 *            The expected {@link JsonToken} type
	 * 
	 * @throws IOException
	 *             If an error occurs while reading the Json.
	 * @throws JsonParseException
	 *             If the current token is not of the expected type or an error occurs while parsing the Json.
	 */
	public void skipToken(JsonToken expectedType) throws JsonParseException, IOException {
		if (this.currentToken() != expectedType)
			throw new JsonParseException("expected token " + expectedType + "; found " + this.currentToken(), null);
		this.nextToken();
	}

}
