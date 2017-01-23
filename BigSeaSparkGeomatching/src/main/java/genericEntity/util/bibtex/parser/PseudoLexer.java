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
 * This is not a real lexer, since <code>BibTex</code> is such an insane format...
 * 
 * @author henkel
 */
final class PseudoLexer {

	/**
	 * The token class that is used within this lexer.
	 */
	static final class Token {

		/**
		 * Creates a new <code>Token</code>.
		 * 
		 * @param choice
		 *            The choice of the token.
		 * @param content
		 *            The content of the token.
		 * @param line
		 *            The line information of the token.
		 * @param column
		 *            The column information of this token.
		 */
		Token(int choice, String content, int line, int column) {
			this.choice = choice;
			this.content = content;
			this.line = line;
			this.column = column;
		}

		/**
		 * The choice of this token.
		 */
		final int choice;

		/**
		 * The content of this token.
		 */
		final String content;

		/**
		 * The line information of this token.
		 */
		final int line;

		/**
		 * The column information of this token.
		 */
		final int column;
	}

	private final LookAheadReader input;
	private Token eofToken = null;

	/**
	 * Initializes the <code>PseudoLexer</code>.
	 * 
	 * @param input
	 *            The reader that is used internally.
	 * @throws IOException
	 *             If an error occurs while reading the data.
	 */
	PseudoLexer(Reader input) throws IOException {
		this.input = new LookAheadReader(input);
	}

	/**
	 * If it's a top-level comment, result.choice will be 0, for @ 1, for EOF 2.
	 * 
	 * @return The next token.
	 * @throws IOException
	 *             If an error occurs while reading the data.
	 */
	public Token scanTopLevelCommentOrAtOrEOF() throws IOException {
		skipWhitespace();
		if (this.eofToken != null) {
			return new Token(2, this.eofToken.content, this.eofToken.line, this.eofToken.column);
		}

		final int column = this.input.getColumn(), line = this.input.getLine();
		if (this.input.getCurrent() == '@') {
			this.input.step();
			return new Token(1, "@", line, column);
		}
		StringBuilder content = new StringBuilder();
		while (!this.input.eof() && this.input.getCurrent() != '@') {
			content.append(this.input.getCurrent());
			this.input.step();
		}
		return new Token(0, content.toString(), line, column);
	}

	/**
	 * The return value is an index into alternatives. If lookAhead is true we will not move forward ...
	 * 
	 * @param alternatives
	 *            <code>?</code>
	 * @param lookAhead
	 *            <code>?</code>
	 * @return int <code>?</code>
	 * @throws IOException
	 *             If an error occurs during the reading process.
	 * @throws ParseException
	 *             If a parsing error occurs.
	 */
	public final int scanAlternatives(char[] alternatives, boolean lookAhead) throws IOException, ParseException {
		skipWhitespace();
		if (this.eofToken != null)
			throw new ParseException(this.eofToken.line, this.eofToken.column, "[EOF]",
					alternativesToString(alternatives));
		final int line = this.input.getLine(), column = this.input.getColumn();
		for (int i = 0; i < alternatives.length; i++) {
			if (alternatives[i] == this.input.getCurrent()) {
				if (!lookAhead)
					this.input.step();
				return i;
			}
		}
		if (!lookAhead)
			this.input.step();
		throw new ParseException(line, column, "" + this.input.getCurrent(), alternativesToString(alternatives));
	}

	// /**
	// * this one is case insensitive!
	// *
	// * @param alternatives
	// * @return Token
	// * @throws ParseException
	// * @throws IOException
	// */
	//
	// public final Token scanAlternatives(String[] alternatives)
	// throws ParseException, IOException {
	// skipWhitespace();
	// if (eofToken != null)
	// throw new ParseException(
	// eofToken.line,
	// eofToken.column,
	// "[EOF]",
	// alternativesToString(alternatives));
	// final int line = input.getLine();
	// final int column = input.getColumn();
	// HashMap amap = new HashMap();
	// int maxLength = 0;
	// for (int i = 0; i < alternatives.length; i++) {
	// amap.put(alternatives[i], new Integer(i));
	// if (alternatives[i].length() > maxLength)
	// maxLength = alternatives[i].length();
	// }
	// String content = "";
	// String lowerCaseContent = "";
	// for (int length = 1; length <= maxLength; length++) {
	// content += input.getCurrent();
	// lowerCaseContent += Character.toLowerCase(input.getCurrent());
	// input.step();
	//
	// if (amap.containsKey(lowerCaseContent)) {
	// return new Token(
	// ((Integer) amap.get(lowerCaseContent)).intValue(),
	// content,
	// line,
	// column);
	// }
	// }
	// throw new ParseException(line, column, content,
	// alternativesToString(alternatives));
	// }

	/**
	 * 
	 * @param terminationSet
	 * @param excludeWhitespace
	 * @param enforceNonzero
	 * @return String
	 * @throws ParseException
	 * @throws IOException
	 */
	public String scanLiteral(char[] terminationSet, boolean excludeWhitespace, boolean enforceNonzero)
			throws ParseException, IOException {
		StringBuilder buffer = new StringBuilder();
		scanLiteral(terminationSet, excludeWhitespace, enforceNonzero, buffer);
		return buffer.toString();
	}

	/**
	 * The return value is an index into the termination set the result is appended in the resultTargetBuffer
	 * 
	 * @param terminationSet
	 * @param excludeWhitespace
	 * @param enforceNonzero
	 * @param resultTargetBuffer
	 * 
	 * @return Token
	 * @throws ParseException
	 * @throws IOException
	 */
	public int scanLiteral(char[] terminationSet, boolean excludeWhitespace, boolean enforceNonzero,
			StringBuilder resultTargetBuffer) throws ParseException, IOException {
		if (excludeWhitespace) {
			skipWhitespace();

			if (this.eofToken != null)
				throw new ParseException(this.eofToken.line, this.eofToken.column, "[EOF]", "not ("
						+ alternativesToString(terminationSet) + " or [whitespace])");
		} else
			enforceNoEof("not (" + alternativesToString(terminationSet) + ")", false);
		final int line = this.input.getLine(), column = this.input.getColumn();
		int indexIntoTerminationSet = -1;
		final int initialResultTargetBufferLength = resultTargetBuffer.length();
		while (true) {
			if (this.input.eof())
				break;
			final char inputChar = this.input.getCurrent();

			indexIntoTerminationSet = index(terminationSet, inputChar);
			if (indexIntoTerminationSet >= 0 || excludeWhitespace && Character.isWhitespace(inputChar)) {
				break;
			}

			this.input.step();
			resultTargetBuffer.append(inputChar);
		}

		if (resultTargetBuffer.length() > initialResultTargetBufferLength || !enforceNonzero) {
			return indexIntoTerminationSet;
		}

		throw new ParseException(line, column, "" + this.input.getCurrent(), "not ("
				+ alternativesToString(terminationSet) + " or [whitespace])");
	}

	private static final char[] QUOTE_OR_LBRACE = new char[] { '\"', '{' };

	/**
	 * @return String
	 * @throws IOException
	 * @throws ParseException
	 */
	public String scanQuotedString() throws IOException, ParseException {
		StringBuilder content = new StringBuilder();
		this.scan('"');
		while (true) {
			final int choice = this.scanLiteral(QUOTE_OR_LBRACE, false, false, content);
			if (choice == 0) { // we terminated with '"'
				break;
			}

			// we found a '{'
			this.scanBracketedString(content, true);
		}

		this.scan('"');
		return content.toString();
	}

	private final char[] RBRACE_LBRACE = new char[] { '}', '{' };

	/**
	 * @param targetBuffer
	 * @param includeOuterBraces
	 * @throws ParseException
	 * @throws IOException
	 */
	public void scanBracketedString(StringBuilder targetBuffer, boolean includeOuterBraces) throws ParseException,
			IOException {
		this.scan('{');
		if (includeOuterBraces)
			targetBuffer.append('{');
		while (true) {
			final int choice = this.scanLiteral(this.RBRACE_LBRACE, false, false, targetBuffer);

			if (choice == 0) { // we terminated with '}'
				break;
			}

			// we terminated with '{'
			this.scanBracketedString(targetBuffer, true);
		}
		this.scan('}');
		if (includeOuterBraces)
			targetBuffer.append('}');
	}

	/**
	 * @return String
	 * @throws ParseException
	 * @throws IOException
	 */
	public String scanEntryTypeName() throws ParseException, IOException {
		skipWhitespace();
		if (this.eofToken != null)
			throw new ParseException(this.eofToken.line, this.eofToken.column, "[EOF]", "[a..z,A..Z]");
		final int line = this.input.getLine(), column = this.input.getColumn();
		StringBuilder result = new StringBuilder();
		while (true) {
			enforceNoEof("[a..z,A..Z]", false);
			char inputChar = this.input.getCurrent();

			if (inputChar >= 'a' && inputChar <= 'z' || inputChar >= 'A' && inputChar <= 'Z') {
				result.append(inputChar);
				this.input.step();
			} else {
				break;
			}
		}
		if (result.length() == 0) {
			throw new ParseException(line, column, "" + this.input.getCurrent(), "[a..z,A..Z]");
		}
		return result.toString();

	}

	/**
	 * @param expected
	 * @throws ParseException
	 * @throws IOException
	 */
	public void scan(char expected) throws ParseException, IOException {
		skipWhitespace();
		if (this.eofToken != null)
			throw new ParseException(this.eofToken.line, this.eofToken.column, "[EOF]", "" + expected);
		final char encountered = this.input.getCurrent();
		if (encountered != expected) {
			final int line = this.input.getLine(), column = this.input.getColumn();
			this.input.step();
			throw new ParseException(line, column, "" + encountered, "" + expected);
		}

		this.input.step();
	}

	/**
	 * @throws IOException
	 */
	public void skipWhitespace() throws IOException {
		if (this.eofToken != null)
			return;
		while (!this.input.eof() && Character.isWhitespace(this.input.getCurrent()))
			this.input.step();
		if (this.input.eof()) {
			this.eofToken = new Token(-1, null, this.input.getLine(), this.input.getColumn());
		}
	}

	/**
	 * make sure you call
	 * 
	 * @param expected
	 * @param skipWhiteSpace
	 * 
	 * @throws ParseException
	 * @throws IOException
	 */
	public void enforceNoEof(String expected, boolean skipWhiteSpace) throws ParseException, IOException {
		if (skipWhiteSpace)
			skipWhitespace();
		else if (this.input.eof()) {
			this.eofToken = new Token(-1, null, this.input.getLine(), this.input.getColumn());
		}
		if (this.eofToken != null)
			throw new ParseException(this.eofToken.line, this.eofToken.column, "[EOF]", "" + expected);
	}

	/**
	 * make sure to query enforceNoEof first!
	 * 
	 * @return char
	 */
	public char currentInputChar() {
		return this.input.getCurrent();
	}

	private static String alternativesToString(char[] alternatives) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("one of ");
		for (int i = 0; i < alternatives.length; i++) {
			if (i != 0)
				buffer.append(',');
			buffer.append('\'');
			buffer.append(alternatives[i]);
			buffer.append('\'');
		}

		return buffer.toString();
	}

	@SuppressWarnings("unused")
	private static String alternativesToString(Object[] alternatives) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("one of ");
		for (int i = 0; i < alternatives.length; i++) {
			if (i != 0)
				buffer.append(',');
			buffer.append('\'');
			buffer.append(alternatives[i]);
			buffer.append('\'');
		}

		return buffer.toString();
	}

	private static int index(char[] container, char element) {
		for (int i = 0; i < container.length; i++) {
			if (container[i] == element)
				return i;
		}
		return -1;

	}
}
