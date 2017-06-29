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

package genericEntity.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import genericEntity.util.data.GenericObject;

/**
 * <code>GlobalConfig</code> manages the configuration parameters of <code>Generic</code>.
 * 
 * @author Matthias Pohl
 */
public class GlobalConfig {

	/**
	 * The default encoding that is used internally.
	 */
	public static final String PARAM_VALUE_DEFAULT_ENCODING = "UTF-8";

	/**
	 * The name of Generic's default encoding.
	 */
	public static final String PARAM_NAME_DEFAULT_ENCODING = "default-encoding";

	/**
	 * The default directory where all files are stored during file-based processing.
	 */
	public static final String PARAM_VALUE_DEFAULT_DIRECTORY = "./extractedData/";

	/**
	 * The name of Generic's default directory.
	 */
	public static final String PARAM_NAME_DEFAULT_DIRECTORY = "default-directory";

	/**
	 * The default setting of the parameter that specifies how many records can be loaded into memory when using the {@link MemoryChecker}.
	 * 
	 * @see MemoryChecker#enoughMemoryAvailable()
	 */
	public static final int PARAM_VALUE_MEMORY_CHECKER_STEP_SIZE = 10000;

	/**
	 * Name of the parameter for specifying how many records can be loaded into memory when using the {@link MemoryChecker}.
	 */
	public static final String PARAM_NAME_MEMORY_CHECKER_STEP_SIZE = "step-size";

	/**
	 * The default maximum memory usage for sorting the data in percent.
	 */
	public static final double PARAM_VALUE_MAXIMUM_MEMORY_USAGE = 0.5;

	/**
	 * Name of the parameter for specifying the maximum memory usage.
	 */
	public static final String PARAM_NAME_MAXIMUM_MEMORY_USAGE = "maximum-memory-usage";

	/**
	 * The default setting of the parameter that specifies if the Json written to the file is formatted or not.
	 */
	public static final boolean PARAM_VALUE_FORMATTED_JSON_OUTPUT = false;

	/**
	 * Name of the parameter for specifying if the formatted Json shall be printed.
	 */
	public static final String PARAM_NAME_FORMATTED_JSON_OUTPUT = "formatted-json-enabled";

	private static final GlobalConfig instance = new GlobalConfig();

	private String defaultEncoding = GlobalConfig.PARAM_VALUE_DEFAULT_ENCODING;
	private String workingDirectoryPath = GlobalConfig.PARAM_VALUE_DEFAULT_DIRECTORY;
	private int memoryCheckerStepSize = GlobalConfig.PARAM_VALUE_MEMORY_CHECKER_STEP_SIZE;
	private double maximumMemoryUsage = GlobalConfig.PARAM_VALUE_MAXIMUM_MEMORY_USAGE;
	private boolean formattedJsonOutputEnabled = GlobalConfig.PARAM_VALUE_FORMATTED_JSON_OUTPUT;

	private GlobalConfig() {
		// nothing else to do
	}

	/**
	 * Returns the Singleton instance of <code>GlobalConfig</code>.
	 * 
	 * @return The Singleton instance.
	 */
	public static GlobalConfig getInstance() {
		return GlobalConfig.instance;
	}

	/**
	 * Returns the default encoding used by GenericObject.
	 * 
	 * @return The default encoding.
	 */
	public String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Sets Generic's default encoding for file-based data-processing.
	 * 
	 * @param defaultEnc
	 *            The new default directory.
	 */
	public void setDefaultEncoding(String defaultEnc) {
		if (defaultEnc == null) {
			throw new NullPointerException("Default encoding is missing.");
		}

		this.defaultEncoding = defaultEnc;
	}

	/**
	 * Returns the working directory path. This directory is used for storing all files that are generated during file-based processing of the data.
	 * 
	 * @return The path of the working directory.
	 */
	public String getWorkingDirectory() {
		this.mkDir(this.workingDirectoryPath);

		return this.workingDirectoryPath;
	}

	// creates the passed directory if it does not already exist
	private void mkDir(String dir) {
		File d = new File(dir);
		if (!d.exists()) {
			d.mkdirs();
		}
	}
	
	public void deleteWorkingDirOnExit() {
		File workingDir = new File(this.workingDirectoryPath);
		this.deleteDirContent(workingDir);
		workingDir.deleteOnExit();
	}
	
	private void deleteDirContent(File f) {
		if (!f.exists()) {
			return;
		}
		
		if (f.isFile()) {
			f.delete();
		} else if (f.isDirectory()) {
			for (File subFile : f.listFiles()) {
				this.deleteDirContent(subFile);
			}
		}
	}

	/**
	 * Sets Generic's default directory for file-based data-processing.
	 * 
	 * @param workingDir
	 *            The new default directory.
	 */
	public void setWorkingDirectory(String workingDir) {
		if (workingDir == null) {
			throw new NullPointerException("Default directory is missing.");
		}

		this.workingDirectoryPath = workingDir;
	}

	/**
	 * Returns the maximum number of {@link GenericObject} that will be stored in memory, if file-based processing is enabled.
	 * 
	 * @return The in-memory object count.
	 * 
	 * @see MemoryChecker
	 */
	public int getInMemoryObjectThreshold() {
		return this.memoryCheckerStepSize;
	}

	/**
	 * Sets the threshold for storing {@link GenericObject}s in memory, if file-based processing is enabled.
	 * 
	 * @param value
	 *            The new threshold.
	 * 
	 * @see MemoryChecker
	 */
	public void setInMemoryObjectThreshold(int value) {
		if (value > 0) {
			this.memoryCheckerStepSize = value;
		} else {
			// default value for dynamic memory usage (@see MemoryChecker)
			this.memoryCheckerStepSize = 0;
		}
	}

	/**
	 * Sets the in-memory object threshold based on the passed String.
	 * 
	 * @param str
	 *            The String that shall be converted in a valid threshold.
	 * @throws NumberFormatException
	 *             If the passed String could not be converted into an <code>Integer</code> value.
	 */
	protected void setInMemoryObjectThreshold(String str) throws NumberFormatException {
		this.setInMemoryObjectThreshold(Integer.parseInt(str));
	}

	/**
	 * Returns the maximum relative memory used during the sorting phase.
	 * 
	 * @return The maximum relative memory usage.
	 */
	public double getMaximumMemoryUsage() {
		return this.maximumMemoryUsage;
	}

	/**
	 * Sets the maximum relative memory that is used for sorting the data. The passed value needs to be within the range of <code>0</code> and
	 * <code>1</code>.
	 * 
	 * @param value
	 *            The maximum relative memory usage.
	 */
	public void setMaximumMemoryUsage(double value) {
		if (value < 0.0) {
			this.maximumMemoryUsage = 0.0;
		} else if (value > 1.0) {
			this.maximumMemoryUsage = 1.0;
		} else {
			this.maximumMemoryUsage = value;
		}
	}

	/**
	 * Sets the maximum relative memory usage based on the passed String.
	 * 
	 * @param str
	 *            The String that shall be converted into a valid <code>Double</code> value.
	 * @throws NumberFormatException
	 *             If the passed String could not be converted into a <code>Double</code> value.
	 */
	protected void setMaximumMemoryUsage(String str) throws NumberFormatException {
		this.setMaximumMemoryUsage(Double.parseDouble(str));
	}

	/**
	 * Checks whether formatted Json is enabled. If this method returns <code>true</code> the Json code will be formatted within each generated Json
	 * file. This makes the Json easier to read but let increase the file size.
	 * 
	 * @return <code>true</code>, if formatted Json is enabled; otherwise <code>false</code>.
	 */
	public boolean formattedJsonIsEnabled() {
		return this.formattedJsonOutputEnabled;
	}

	/**
	 * Enables formatted Json.
	 */
	public void enableFormattedJson() {
		this.formattedJsonOutputEnabled = true;
	}

	/**
	 * Disables formatted Json.
	 */
	public void disableFormattedJson() {
		this.formattedJsonOutputEnabled = false;
	}

	/**
	 * Loads the configuration from the passed {@link Properties}.
	 * 
	 * @param properties
	 *            The new parameters.
	 * @return <code>true</code>, if all parameters were set to the new values; otherwise <code>false</code>.
	 */
	public boolean loadConfig(Properties properties) {
		boolean parametersChanged = true;

		if (properties.containsKey(GlobalConfig.PARAM_NAME_DEFAULT_ENCODING)) {
			this.setDefaultEncoding(properties.getProperty(GlobalConfig.PARAM_NAME_DEFAULT_ENCODING));
		} else {
			parametersChanged = false;
		}

		if (properties.containsKey(GlobalConfig.PARAM_NAME_DEFAULT_DIRECTORY)) {
			this.setWorkingDirectory(properties.getProperty(GlobalConfig.PARAM_NAME_DEFAULT_DIRECTORY));
		} else {
			parametersChanged = false;
		}

		if (properties.containsKey(GlobalConfig.PARAM_NAME_MEMORY_CHECKER_STEP_SIZE)) {
			this.setInMemoryObjectThreshold(Integer.parseInt(properties.getProperty(GlobalConfig.PARAM_NAME_MEMORY_CHECKER_STEP_SIZE)));
		} else {
			parametersChanged = false;
		}

		if (properties.containsKey(GlobalConfig.PARAM_NAME_MAXIMUM_MEMORY_USAGE)) {
			this.setMaximumMemoryUsage(Double.parseDouble(properties.getProperty(GlobalConfig.PARAM_NAME_MAXIMUM_MEMORY_USAGE)));
		} else {
			parametersChanged = false;
		}

		if (properties.containsKey(GlobalConfig.PARAM_NAME_FORMATTED_JSON_OUTPUT)) {
			if (properties.getProperty(GlobalConfig.PARAM_NAME_FORMATTED_JSON_OUTPUT).equals("1")
					|| properties.getProperty(GlobalConfig.PARAM_NAME_FORMATTED_JSON_OUTPUT).equals("true")) {
				this.enableFormattedJson();
			} else {
				this.disableFormattedJson();
			}
		} else {
			parametersChanged = false;
		}

		return parametersChanged;
	}

	/**
	 * Persists the current configuration using the {@link Properties} format.
	 * 
	 * @param outputStream
	 *            The stream onto which the configuration will be written.
	 * @throws IOException
	 *             If an error occurs during the writing process.
	 */
	public void saveConfig(OutputStream outputStream) throws IOException {
		Properties properties = new Properties();
		properties.setProperty(GlobalConfig.PARAM_NAME_DEFAULT_ENCODING, this.getDefaultEncoding());
		properties.setProperty(GlobalConfig.PARAM_NAME_DEFAULT_DIRECTORY, this.getWorkingDirectory());
		properties.setProperty(GlobalConfig.PARAM_NAME_MEMORY_CHECKER_STEP_SIZE, String.valueOf(this.getInMemoryObjectThreshold()));
		properties.setProperty(GlobalConfig.PARAM_NAME_MAXIMUM_MEMORY_USAGE, String.valueOf(this.getMaximumMemoryUsage()));
		properties.setProperty(GlobalConfig.PARAM_NAME_FORMATTED_JSON_OUTPUT, this.formattedJsonIsEnabled() ? "1" : "0");
		properties.store(outputStream, "GenericObject configuration");
	}

	private String getCommandLineParameter(String str) {
		return "--" + str;
	}

	/**
	 * Sets the global configuration based on the command-line arguments.
	 * 
	 * @param args
	 *            The command-line arguments String array that was passed through the <code>main</code> method parameters.
	 * @throws IllegalArgumentException
	 *             If an error occurs while parsing the arguments.
	 */
	public void setCommandLineArguments(String[] args) throws IllegalArgumentException {
		for (int i = 0; i < args.length; ++i) {
			final String cmdLineParamDefaultEncoding = this.getCommandLineParameter(GlobalConfig.PARAM_NAME_DEFAULT_ENCODING);
			final String cmdLineParamDefaultDirectory = this.getCommandLineParameter(GlobalConfig.PARAM_NAME_DEFAULT_DIRECTORY);
			final String cmdLineParamFormattedJson = this.getCommandLineParameter(GlobalConfig.PARAM_NAME_FORMATTED_JSON_OUTPUT);
			final String cmdLineParamMemCheckerStepSize = this.getCommandLineParameter(GlobalConfig.PARAM_NAME_MEMORY_CHECKER_STEP_SIZE);
			final String cmdLineParamMaxMemoryUsage = this.getCommandLineParameter(GlobalConfig.PARAM_NAME_MAXIMUM_MEMORY_USAGE);

			if (cmdLineParamFormattedJson.equals(args[i])) {
				this.enableFormattedJson();
			} else if (cmdLineParamMemCheckerStepSize.equals(args[i])) {
				if (i + 1 >= args.length) {
					throw new IllegalArgumentException("The step size value is missing.");
				}

				try {
					this.setInMemoryObjectThreshold(args[i + 1]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The value followed by '" + cmdLineParamMemCheckerStepSize
							+ "' couldn't be parsed into an integer value.");
				}
			} else if (cmdLineParamMaxMemoryUsage.equals(args[i])) {
				if (i + 1 >= args.length) {
					throw new IllegalArgumentException("The maximum relative memory usage is missing.");
				}

				try {
					this.setMaximumMemoryUsage(args[i + 1]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The value followed by '" + cmdLineParamMaxMemoryUsage
							+ "' couldn't be parsed into a double value.");
				}
			} else if (cmdLineParamDefaultDirectory.equals(args[i])) {
				if (i + 1 >= args.length) {
					throw new IllegalArgumentException("The default directory value is missing.");
				}

				this.setWorkingDirectory(args[i + 1]);
			} else if (cmdLineParamDefaultEncoding.equals(args[i])) {
				if (i + 1 >= args.length) {
					throw new IllegalArgumentException("The default encoding value is missing.");
				}

				this.setDefaultEncoding(args[i + 1]);
			}
		}
	}
}
