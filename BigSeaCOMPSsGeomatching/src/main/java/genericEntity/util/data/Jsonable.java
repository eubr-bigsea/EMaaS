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

package genericEntity.util.data;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;

import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;

/**
 * <code>Jsonable</code> can be used by classes whose instances shall be Json-convertible.
 * 
 * @author Matthias Pohl
 */
public interface Jsonable extends AutoJsonable {

	/**
	 * Generates the Json code using the passed {@link GenericJsonGenerator}.
	 * 
	 * @param jsonGenerator
	 *            The <code>GenericJsonGenerator</code> that is used internally.
	 * @throws JsonGenerationException
	 *             If an error occurs while generating the Json syntax.
	 * @throws IOException
	 *             If an error occurs while writing to the output.
	 */
	public void toJson(GenericJsonGenerator jsonGenerator) throws JsonGenerationException, IOException;

	/**
	 * Initializes the current instance using the passed {@link GenericJsonParser}.
	 * 
	 * @param jsonParser
	 *            The parser that is used for extracting the data out of the Json.
	 * @throws IOException
	 *             If an error occurs while reading from the stream.
	 * @throws JsonParseException
	 *             If an error occurs while parsing the Json.
	 */
	public void fromJson(GenericJsonParser<?> jsonParser) throws JsonParseException, IOException;
}
