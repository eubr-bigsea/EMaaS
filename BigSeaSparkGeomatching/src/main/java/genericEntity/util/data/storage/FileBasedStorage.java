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

package genericEntity.util.data.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;

import genericEntity.util.GlobalConfig;
import genericEntity.util.data.Jsonable;
import genericEntity.util.data.json.GenericJsonGenerator;
import genericEntity.util.data.json.GenericJsonParser;
import genericEntity.util.data.json.JsonValue;

/**
 * <code>FileBasedStorage</code> stores {@link Jsonable} instances in files.
 * 
 * @author Matthias Pohl
 * 
 * @param <T>
 *            The <code>Jsonable</code> type whose instances are stored.
 */
public class FileBasedStorage<T extends Jsonable> extends AbstractGenericObjectStorage<T> {

	private static final Logger logger = Logger.getLogger(FileBasedStorage.class.getPackage().getName());

	/**
	 * The file extension that is used for each file.
	 */
	public static final String JSON_FILE_EXTENSION = ".json";

	private class FileBasedReader implements JsonableReader<T> {

		private Collection<Reader> openedReaders = new ArrayList<Reader>();

		@Override
		public Iterator<T> iterator() {
			try {
				final Reader reader = new BufferedReader(new FileReader(FileBasedStorage.this.file));
				this.openedReaders.add(reader);

				return new GenericJsonParser<T>(FileBasedStorage.this.internalType, reader);
			} catch (FileNotFoundException e) {
				FileBasedStorage.logger.warn("'" + FileBasedStorage.this.file.getAbsolutePath() + "' was not found.", e);
			} catch (IOException e) {
				FileBasedStorage.logger.warn("An error occurred while initializing the iterator for '" + FileBasedStorage.this.file.getAbsolutePath()
						+ "'.", e);
			}

			return null;
		}

		@Override
		public void close() throws IOException {
			IOException propagatedException = null;
			for (Reader reader : this.openedReaders) {
				try {
					reader.close();
				} catch (IOException e) {
					propagatedException = e;
				}
			}

			if (propagatedException != null) {
				throw propagatedException;
			}
		}

	}

	private class FileBasedWriter extends AbstractJsonableWriter<T> {

		private GenericJsonGenerator generator = new GenericJsonGenerator(new BufferedWriter(new FileWriter(FileBasedStorage.this.file)));

		private FileBasedWriter() throws IOException {
			if (FileBasedStorage.this.isFormattedJson()) {
				this.generator.enableFormattedJson();
			} else {
				this.generator.disableFormattedJson();
			}

			this.generator.writeArrayStart();
			FileBasedStorage.this.size = 0;
		}

		@Override
		public boolean add(T value) throws JsonGenerationException, IOException {
			if (this.generator == null) {
				throw new IllegalStateException("The writer was already closed.");
			}

			value.toJson(this.generator);
			FileBasedStorage.this.size++;

			return true;
		}

		@Override
		public void close() throws IOException {
			if (this.generator != null) {
				this.generator.writeArrayEnd();
				this.generator.close();

				this.generator = null;
			}
		}

	}

	private File file;

	private int size = -1;

	private Class<T> internalType;

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the passed name. The underlying file will be placed in the default directory. The
	 * reader will return {@link JsonValue} instances.
	 * 
	 * @param name
	 *            The name of the underlying file.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 * 
	 * @see {@link GlobalConfig#getWorkingDirectory()}
	 */
	public FileBasedStorage(String name) throws IOException {
		this(null, GlobalConfig.getInstance().getWorkingDirectory(), name);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the passed type information and a name. The underlying file will be placed in the
	 * default directory.
	 * 
	 * @param type
	 *            The type of instances that are stored. This type is equal to the generic type of this instance.
	 * @param name
	 *            The name of the underlying file.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 * 
	 * @see {@link GlobalConfig#getWorkingDirectory()}
	 */
	public FileBasedStorage(Class<T> type, String name) throws IOException {
		this(type, GlobalConfig.getInstance().getWorkingDirectory(), name);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the passed name, and the initial content. The underlying file will be placed in the
	 * default directory. The initial content will be overwritten if {@link #getWriter()} is called. The return type of the {@link JsonableReader} is
	 * {@link JsonValue}.
	 * 
	 * @param name
	 *            The name of the underlying file.
	 * @param initialContent
	 *            The initial content that shall be added to that storage.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 * 
	 * @see {@link GlobalConfig#getWorkingDirectory()}
	 */
	public FileBasedStorage(String name, Collection<T> initialContent) throws IOException {
		this(null, GlobalConfig.getInstance().getWorkingDirectory(), name, initialContent);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the passed type information, its name, and the initial content. The underlying file
	 * will be placed in the default directory. The initial content will be overwritten if {@link #getWriter()} is called.
	 * 
	 * @param type
	 *            The type of instances that are stored. This type is equal to the generic type of this instance.
	 * @param name
	 *            The name of the underlying file.
	 * @param initialContent
	 *            The initial content that shall be added to that storage.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 * 
	 * @see {@link GlobalConfig#getWorkingDirectory()}
	 */
	public FileBasedStorage(Class<T> type, String name, Collection<T> initialContent) throws IOException {
		this(type, GlobalConfig.getInstance().getWorkingDirectory(), name, initialContent);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the type information, a directory and its name. The return type of the
	 * {@link JsonableReader} is {@link JsonValue} since no type is passed.
	 * 
	 * @param dir
	 *            The directory where the file shall be placed in.
	 * @param name
	 *            The name of the underlying file.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 */
	public FileBasedStorage(String dir, String name) throws IOException {
		this(null, dir, name, null);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the type information, a directory and its name.
	 * 
	 * @param type
	 *            The type of instances that are stored. This type is equal to the generic type of this instance.
	 * @param dir
	 *            The directory where the file shall be placed in.
	 * @param name
	 *            The name of the underlying file.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 */
	public FileBasedStorage(Class<T> type, String dir, String name) throws IOException {
		this(type, dir, name, null);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the type information, a directory, its name, and the initial content. The initial
	 * content will be overwritten if {@link #getWriter()} is called.The return type of the {@link JsonableReader} is {@link JsonValue} since no type
	 * is passed.
	 * 
	 * @param dir
	 *            The directory where the file shall be placed in.
	 * @param name
	 *            The name of the underlying file.
	 * @param initialContent
	 *            The initial content that shall be added to that storage.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 */
	public FileBasedStorage(String dir, String name, Collection<T> initialContent) throws IOException {
		this(null, new File(dir, name + FileBasedStorage.JSON_FILE_EXTENSION), initialContent);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the type information, a directory, its name, and the initial content. The initial
	 * content will be overwritten if {@link #getWriter()} is called.
	 * 
	 * @param type
	 *            The type of instances that are stored. This type is equal to the generic type of this instance.
	 * @param dir
	 *            The directory where the file shall be placed in.
	 * @param name
	 *            The name of the underlying file.
	 * @param initialContent
	 *            The initial content that shall be added to that storage.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 */
	public FileBasedStorage(Class<T> type, String dir, String name, Collection<T> initialContent) throws IOException {
		this(type, new File(dir, name + FileBasedStorage.JSON_FILE_EXTENSION), initialContent);
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the type information, the underlying file, and the initial content. The initial
	 * content will be overwritten {@link #getWriter()} is called.
	 * 
	 * @param type
	 *            The type of instances that are stored. This type is equal to the generic type of this instance.
	 * @param f
	 *            The underlying file.
	 * @param initialContent
	 *            The initial content that shall be added to that storage.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 */
	protected FileBasedStorage(Class<T> type, File f, Collection<T> initialContent) throws IOException {
		super();

		this.file = f;
		this.internalType = type;

		if (!f.exists()) {
			this.generateEmptyJsonFile();
		}

		if (initialContent != null) {
			JsonableWriter<T> initialWriter = this.getWriter();

			for (T element : initialContent) {
				initialWriter.add(element);
			}

			initialWriter.close();
		}
	}

	/**
	 * Initializes a <code>FileBasedStorage</code> instance with the type information, the underlying file, and the initial content. The initial
	 * content will be overwritten {@link #getWriter()} is called.
	 * 
	 * @param type
	 *            The type of instances that are stored. This type is equal to the generic type of this instance.
	 * @param f
	 *            The underlying file.
	 * @throws IOException
	 *             If an error occurs while initializing the file.
	 */
	public FileBasedStorage(Class<T> type, File f) throws IOException {
		this(type, f, null);
	}

	/**
	 * Overwrites any existing file and initializes a new one containing an empty Json array.
	 * 
	 * @throws IOException
	 *             If an error occurs while initializing the new file.
	 */
	protected void generateEmptyJsonFile() throws IOException {
		this.file.createNewFile();

		final Writer writer = new FileWriter(this.file);
		writer.write("[]");
		writer.close();
	}

	@Override
	public JsonableReader<T> getReader() {
		return new FileBasedReader();
	}

	@Override
	public JsonableWriter<T> getWriter() throws IOException {
		return new FileBasedWriter();
	}

	private void initializeSize() throws IOException {
		JsonableReader<T> reader = this.getReader();

		Iterator<T> iterator = reader.iterator();
		for (this.size = 0; iterator.hasNext(); this.size++) {
			iterator.next();
		}

		reader.close();
	}

	@Override
	public int size() {
		if (this.size == -1) {
			try {
				this.initializeSize();
			} catch (IOException e) {
				throw new IllegalStateException("An IOException occurred while iterating over the file content.", e);
			}
		}

		return this.size;
	}

	/**
	 * Renames the underlying file.
	 * 
	 * @param newFilename
	 *            The new name of the file.
	 */
	public void renameTo(String newFilename) {
		final File oldFile = this.file;
		this.file = new File(oldFile.getParent(), newFilename + FileBasedStorage.JSON_FILE_EXTENSION);
		if (!oldFile.renameTo(this.file)) {
			FileBasedStorage.logger.warn("Unable to rename file '" + oldFile.getName() + "' to '" + newFilename
					+ FileBasedStorage.JSON_FILE_EXTENSION + "'.");
		}
	}

	/**
	 * Returns the underlying file.
	 * 
	 * @return the underlying file
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Deletes the underlying file.
	 */
	public void delete() {
		this.file.delete();
	}

}
