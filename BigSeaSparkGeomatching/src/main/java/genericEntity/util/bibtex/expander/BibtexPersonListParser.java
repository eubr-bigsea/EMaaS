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
 * Created on Mar 27, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package genericEntity.util.bibtex.expander;

import java.util.Collection;
import java.util.LinkedList;

import genericEntity.util.bibtex.data.BibtexFile;
import genericEntity.util.bibtex.data.BibtexPerson;
import genericEntity.util.bibtex.data.BibtexPersonList;
import genericEntity.util.bibtex.data.BibtexString;

/**
 * @author henkel
 */
final class BibtexPersonListParser {

	static final class StringIterator {

		private final char[] chars;

		private int pos;

		StringIterator(String string) {
			this.chars = string.toCharArray();
			this.pos = 0;
		}

		char next() {
			return this.chars[this.pos++];
		}

		char current() {
			return this.chars[this.pos];
		}

		void step() {
			this.pos++;
		}

		void skipWhiteSpace() {
			while (this.pos < this.chars.length && Character.isWhitespace(this.chars[this.pos]))
				this.pos++;
		}

		boolean hasNext() {
			return this.pos + 1 < this.chars.length;
		}
	}

	public static BibtexPersonList parse(BibtexString personList, String entryKey) throws PersonListParserException {

		String content = personList.getContent();
		String[] tokens = tokenize(content);

		BibtexPersonList result = personList.getOwnerFile().makePersonList();
		if (tokens.length == 0) {
			return result;
		}
		int begin = 0;
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].toLowerCase().equals(AND) && begin < i) {
				result.add(makePerson(tokens, begin, i, content, entryKey, personList.getOwnerFile()));
				begin = i + 1;
			}
		}
		if (begin < tokens.length)
			result.add(makePerson(tokens, begin, tokens.length, content, entryKey, personList.getOwnerFile()));
		return result;
	}

	private static boolean firstCharAtBracelevel0IsLowerCase(final String string) {
		StringIterator stringIt = new StringIterator(string);
		if (Character.isLowerCase(stringIt.current()))
			return true;
		while (stringIt.hasNext()) {
			stringIt.skipWhiteSpace();
			if (Character.isLowerCase(stringIt.current()))
				return true;
			if (Character.isUpperCase(stringIt.current()))
				return false;
			if (stringIt.current() == '{') {
				stringIt.step();
				stringIt.skipWhiteSpace();

				if (stringIt.current() == '\\') {
					scanCommandOrAccent: while (true) {
						stringIt.step();
						stringIt.skipWhiteSpace();
						if (Character.isLetter(stringIt.current())) {
							while (stringIt.hasNext() && stringIt.current() != '{' && !Character.isWhitespace(stringIt.current())
									&& stringIt.current() != '}')
								stringIt.step();
							stringIt.skipWhiteSpace();
							if (!stringIt.hasNext())
								return false;
							if (stringIt.current() == '}')
								return false;
							if (stringIt.current() == '{') {
								stringIt.step();
								stringIt.skipWhiteSpace();
								if (!stringIt.hasNext())
									return false;
								if (Character.isLowerCase(stringIt.current()))
									return true;
								if (Character.isUpperCase(stringIt.current()))
									return false;
								if (stringIt.current() == '\\') {
									continue scanCommandOrAccent;
								}
							}
						} else {
							while (stringIt.hasNext() && !Character.isLetter(stringIt.current())) {
								stringIt.step();
							}
							if (!stringIt.hasNext())
								return false;
							if (Character.isLowerCase(stringIt.current()))
								return true;
							if (Character.isUpperCase(stringIt.current()))
								return false;
							return false;
						}
					}
				}
				
					// brace level 1
					int braces = 1;
					while (braces > 0) {
						if (!stringIt.hasNext())
							return false;
						else if (stringIt.current() == '{')
							braces++;
						else if (stringIt.current() == '}')
							braces--;
						stringIt.step();
					}
					// back at brace level 0
			} else {
				stringIt.step();
			}
		}
		return false;
	}

	private static String getString(String[] tokens, int beginIndex, int endIndex) {
		if (!(beginIndex < endIndex))
			return null;
		assert beginIndex >= 0;
		assert endIndex >= 0;
		StringBuffer result = new StringBuffer();
		for (int i = beginIndex; i < endIndex; i++) {
			if (tokens[i] == MINUS) {
				if (i == beginIndex || i == endIndex - 1)
					continue;
				result.append('-');
				continue;
			}
			if (i > beginIndex && tokens[i - 1] != MINUS)
				result.append(' ');
			result.append(tokens[i]);
		}
		return result.toString();
	}

	private static BibtexPerson makePerson(String[] tokens, int begin, int end, String fullEntry, String entryKey, BibtexFile factory)
			throws PersonListParserException {
		if (tokens[begin].equals("others")) {
			return factory.makePerson(null, null, null, null, true);
		} else if (tokens[end - 1] == COMMA)
			throw new PersonListParserException("Name ends with comma: '" + fullEntry + "' - in '" + entryKey + "'");
		else {
			int numberOfCommas = 0;
			for (int i = begin; i < end; i++) {
				if (tokens[i] == COMMA)
					numberOfCommas++;
			}
			if (numberOfCommas == 0) {
				int lastNameBegin = end - 1;
				while (true) {
					if (lastNameBegin - 1 >= begin && !firstCharAtBracelevel0IsLowerCase(tokens[lastNameBegin - 1])) {
						lastNameBegin -= 1;
					} else if (lastNameBegin - 2 >= begin && tokens[lastNameBegin - 1] == MINUS
							&& !firstCharAtBracelevel0IsLowerCase(tokens[lastNameBegin - 2])) {
						lastNameBegin -= 2;
					} else
						break;
				}
				int firstLowerCase = -1;
				for (int i = begin; i < end; i++) {
					if (tokens[i] == MINUS)
						continue;
					if (firstCharAtBracelevel0IsLowerCase(tokens[i])) {
						firstLowerCase = i;
						break;
					}
				}
				final String last, first, lineage, preLast;
				if (lastNameBegin == begin || firstLowerCase == -1) {
					// there is no preLast part

					lastNameBegin = end - 1;
					while (lastNameBegin - 2 >= begin && tokens[lastNameBegin - 1] == MINUS
							&& !firstCharAtBracelevel0IsLowerCase(tokens[lastNameBegin - 2]))
						lastNameBegin -= 2;
					last = getString(tokens, lastNameBegin, end);
					first = getString(tokens, begin, lastNameBegin);
					lineage = null;
					preLast = null;
				} else {
					last = getString(tokens, lastNameBegin, end);
					first = getString(tokens, begin, firstLowerCase);
					lineage = null;
					preLast = getString(tokens, firstLowerCase, lastNameBegin);
				}
				if (last == null)
					throw new PersonListParserException("Found an empty last name in '" + fullEntry + "' in '" + entryKey + "'.");
				return factory.makePerson(first, preLast, last, lineage, false);
			} else if (numberOfCommas == 1 || numberOfCommas == 2) {

				if (numberOfCommas == 1) {
					int commaIndex = -1;
					for (int i = begin; i < end; i++) {
						if (tokens[i] == COMMA) {
							commaIndex = i;
							break;
						}
					}
					final int preLastBegin = begin;
					int preLastEnd = begin;
					for (int i = preLastEnd; i < commaIndex; i++) {
						if (tokens[i] == MINUS)
							continue;
						if (firstCharAtBracelevel0IsLowerCase(tokens[i])) {
							preLastEnd = i + 1;
						}
					}
					if (preLastEnd == commaIndex && preLastEnd > preLastBegin) {
						preLastEnd--;
					}
					final String preLast = getString(tokens, preLastBegin, preLastEnd);
					final String last = getString(tokens, preLastEnd, commaIndex);
					final String first = getString(tokens, commaIndex + 1, end);
					if (last == null)
						throw new PersonListParserException("Found an empty last name in '" + fullEntry + "' in '" + entryKey + "'.");
					return factory.makePerson(first, preLast, last, null, false);
				}
				// 2 commas ...
				int firstComma = -1;
				int secondComma = -1;
				for (int i = begin; i < end; i++) {
					if (tokens[i] == COMMA) {
						if (firstComma == -1) {
							firstComma = i;
						} else {
							secondComma = i;
							break;
						}
					}
				}
				final int preLastBegin = begin;
				int preLastEnd = begin;
				for (int i = preLastEnd; i < firstComma; i++) {
					if (tokens[i] == MINUS)
						continue;
					if (firstCharAtBracelevel0IsLowerCase(tokens[i])) {
						preLastEnd = i + 1;
					}
				}
				if (preLastEnd == firstComma && preLastEnd > preLastBegin) {
					preLastEnd--;
				}
				final String preLast = getString(tokens, preLastBegin, preLastEnd);
				final String last = getString(tokens, preLastEnd, firstComma);
				String lineage = getString(tokens, firstComma + 1, secondComma);
				String first = getString(tokens, secondComma + 1, end);
				if (first == null && lineage != null) {
					String tmp = lineage;
					lineage = first;
					first = tmp;
				}
				if (last == null)
					throw new PersonListParserException("Found an empty last name in '" + fullEntry + "' in '" + entryKey + "'.");
				return factory.makePerson(first, preLast, last, lineage, false);
			} else {
				throw new PersonListParserException("Too many commas in '" + fullEntry + "' in '" + entryKey + "'.");
			}
		}
	}

	private static final String COMMA = ",".intern();

	private static final String AND = "and".intern();

	private static final String MINUS = "-".intern();

	/**
	 * 
	 * 
	 * @param stringContent
	 * @return String[]
	 */
	private static String[] tokenize(String stringContent) {
		int numberOfOpenBraces = 0;
		int tokenBegin = 0;
		final String modifiedStringContent = stringContent + " ";
		// make sure the last character is whitespace ;-)
		Collection<String> tokens = new LinkedList<String>(); // just some strings ...
		for (int currentPos = 0; currentPos < modifiedStringContent.length(); currentPos++) {
			switch (modifiedStringContent.charAt(currentPos)) {
			case '{':
				numberOfOpenBraces++;
				break;
			case '}':
				if (numberOfOpenBraces > 0) {
					numberOfOpenBraces--;
				} else {
					if (tokenBegin <= currentPos - 1) {
						String potentialToken = modifiedStringContent.substring(tokenBegin, currentPos).trim();
						if (!potentialToken.equals("")) {
							tokens.add(potentialToken);
						}
					}
					tokenBegin = currentPos + 1;
				}
				break;
			case ',':
				if (numberOfOpenBraces == 0) {
					if (tokenBegin <= currentPos - 1) {
						String potentialToken = modifiedStringContent.substring(tokenBegin, currentPos).trim();
						if (!potentialToken.equals("")) {
							tokens.add(potentialToken);
						}
					}
					tokens.add(BibtexPersonListParser.COMMA);
					tokenBegin = currentPos + 1;
				}
				break;
			default:
				char currentChar = modifiedStringContent.charAt(currentPos);
				if (Character.isWhitespace(currentChar) || (currentChar == '~') || (currentChar == '-')) {
					if (numberOfOpenBraces == 0 && tokenBegin <= currentPos) {
						String potentialToken = modifiedStringContent.substring(tokenBegin, currentPos).trim();
						if (!potentialToken.equals("")) {
							tokens.add(potentialToken);
							if (currentChar == '-')
								tokens.add(MINUS);
						}
						tokenBegin = currentPos + 1;
					}
				}
			}
		}
		String[] result = new String[tokens.size()];
		tokens.toArray(result);
		return result;
	}
}