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
import java.util.LinkedList;

import genericEntity.util.bibtex.data.BibtexAbstractValue;
import genericEntity.util.bibtex.data.BibtexEntry;
import genericEntity.util.bibtex.data.BibtexFile;

/**
 * The parser will parse the <code>BibTex</code> into a basic AST. Have a look at the different Expanders defined in the
 * bibtexexpansions package if you need more than that.
 * 
 * @author henkel
 */
public final class BibtexParser {

	/**
	 * Creates a new <code>BibTexParser</code>.
	 * 
	 * @param throwAllParseExceptions
	 *            Setting this to <code>true</code> means that all exceptions will be thrown immediately. Otherwise, the
	 *            parser will skip over things it can't parse and you can use {@link #getExceptions()} to retrieve the
	 *            exceptions later.
	 */
	public BibtexParser(boolean throwAllParseExceptions) {
		this.throwAllParseExceptions = throwAllParseExceptions;
	}

	private PseudoLexer lexer;
	private BibtexFile bibtexFile;
	private LinkedList<ParseException> exceptions;
	private boolean throwAllParseExceptions;

	/**
	 * Returns the list of non-fatal exceptions that occurred during parsing. Usually, these occur while parsing an
	 * entry. Usually, the remainder of the entry will be treated as part of a comment - thus the following entry will
	 * be parsed again.
	 * 
	 * @return An array of {@link ParseException}s.
	 */
	public ParseException[] getExceptions() {
		if (this.exceptions == null)
			return new ParseException[0];
		ParseException[] result = new ParseException[this.exceptions.size()];
		this.exceptions.toArray(result);
		return result;
	}

	/**
	 * Parses the input into {@link #bibtexFile} - don't forget to check {@link #getExceptions()} afterwards (if you
	 * don't use <code>throwAllParseExceptions</code> which you can configure in the constructor)...
	 * 
	 * @param bibtexFile
	 *            The <code>Java</code> representation of the file.
	 * @param input
	 *            The reader that is used for reading from the file.
	 * @throws ParseException
	 *             If an entry couldn't be parsed. This exception won't be thrown if
	 *             <code>throwAllParseExceptions</code> was disabled within the constructor.
	 * @throws IOException
	 *             If another error occurs while reading.
	 */

	public void parse(BibtexFile bibtexFile, Reader input) throws ParseException, IOException {
		this.lexer = new PseudoLexer(input);
		this.bibtexFile = bibtexFile;
		this.exceptions = new LinkedList<ParseException>();
		while (true) {
			PseudoLexer.Token token = this.lexer.scanTopLevelCommentOrAtOrEOF();
			switch (token.choice) {
			case 0: // top level comment
				bibtexFile.addEntry(bibtexFile.makeToplevelComment(token.content));
				break;
			case 1: // @ sign
				if (this.throwAllParseExceptions)
					this.parseEntry();
				else {
					try {
						this.parseEntry();
					} catch (ParseException parseException) {
						this.exceptions.add(parseException);
					}
				}
				break;
			case 2: // EOF
				return;
			}
		}
	}

	private final static char[] EXCEPTION_SET_NAMES = new char[] { '"', '#', '%', '\'', '(', ')', ',', '=', '{', '}' };

	@SuppressWarnings("unused")
	private final static String[] ENTRY_TYPES = new String[] { "string", "preamble", "article", "book", "booklet",
			"conference", "inbook", "incollection", "inproceedings", "manual", "mastersthesis", "misc", "phdthesis",
			"proceedings", "techreport", "unpublished", "periodical" // not really standard but commonly used.
	};

	private void parseEntry() throws ParseException, IOException {
		String entryType = this.lexer.scanEntryTypeName().toLowerCase();
		final int bracketChoice = this.lexer.scanAlternatives(new char[] { '{', '(' }, false);

		if (entryType.equals("string")) {
			String stringName = this.lexer.scanLiteral(EXCEPTION_SET_NAMES, true, true);
			this.lexer.scan('=');
			BibtexAbstractValue value = parseValue();
			this.bibtexFile.addEntry(this.bibtexFile.makeMacroDefinition(stringName, value));
		} else if (entryType.equals("preamble")) {
			BibtexAbstractValue value = parseValue();
			this.bibtexFile.addEntry(this.bibtexFile.makePreamble(value));
		} else { // all others
			this.lexer.skipWhitespace();
			String bibkey = (this.lexer.currentInputChar() == ',') ? "" : this.lexer.scanLiteral(new char[] { ',' },
					true, true);
			final BibtexEntry entry = this.bibtexFile.makeEntry(entryType, bibkey);
			this.bibtexFile.addEntry(entry);
			while (true) {
				this.lexer.enforceNoEof("',' or corresponding closing bracket", true);
				// System.out.println("---------->'"+lexer.currentInputChar()+"'");
				if (this.lexer.currentInputChar() == ',') {
					this.lexer.scan(',');
					this.lexer.enforceNoEof("'}' or [FIELDNAME]", true);
					if (this.lexer.currentInputChar() == '}')
						break;
					String fieldName = this.lexer.scanLiteral(EXCEPTION_SET_NAMES, true, true);
					this.lexer.scan('=');
					BibtexAbstractValue value = parseValue();
					entry.setField(fieldName, value);
				} else
					break;
			}
		}

		if (bracketChoice == 0)
			this.lexer.scan('}');
		else
			this.lexer.scan(')');
	}

	private static boolean isNumber(String string) {
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < '0' || '9' < c)
				return false;
		}
		return true;
	}

	private BibtexAbstractValue parseValue() throws ParseException, IOException {
		this.lexer.enforceNoEof("[STRING] or [STRINGREFERENCE] or [NUMBER]", true);
		char inputCharacter = this.lexer.currentInputChar();
		BibtexAbstractValue result;

		if (inputCharacter == '"') {
			result = this.parseQuotedString();
		} else if (inputCharacter == '{') {
			result = this.parseBracketedString();
		} else {
			String stringContent = this.lexer.scanLiteral(BibtexParser.EXCEPTION_SET_NAMES, false, true).trim();
			if (isNumber(stringContent))
				result = this.bibtexFile.makeString(stringContent);
			else
				result = this.bibtexFile.makeMacroReference(stringContent);
		}

		this.lexer.enforceNoEof("'#' or something else", true);
		if (this.lexer.currentInputChar() == '#') {
			this.lexer.scan('#');
			return this.bibtexFile.makeConcatenatedValue(result, parseValue());
		}

		return result;
	}

	private BibtexAbstractValue parseBracketedString() throws ParseException, IOException {
		StringBuilder buffer = new StringBuilder();
		this.lexer.scanBracketedString(buffer, false);
		return this.bibtexFile.makeString(buffer.toString());
	}

	private BibtexAbstractValue parseQuotedString() throws IOException, ParseException {
		return this.bibtexFile.makeString(this.lexer.scanQuotedString());
	}

}
