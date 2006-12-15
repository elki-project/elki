package de.lmu.ifi.dbs.utilities.optionhandling;

import java.io.File;
import java.util.Vector;

/**
 * Parameter class for a parameter specifying a list of files.
 * 
 * @author Steffi Wanka
 *
 */
public class FileListParameter extends ListParameter<File> {

	/**
	 * Specifies the file type, i.e. if the file is an input or output file.
	 */
	private int fileType;

	/**
	 * Constructs a file list parameter with the given name, description, and file type
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param fileType the file type of this file list parameter
	 */
	public FileListParameter(String name, String description, int fileType) {
		super(name, description);
		this.fileType = fileType;
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {

			String[] files = SPLIT.split(value);
			Vector<File> fileValue = new Vector<File>();
			for (String f : files) {
				fileValue.add(new File(f));
			}
			this.value = fileValue;
		}
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	public boolean isValid(String value) throws ParameterException {

		String[] files = SPLIT.split(value);
		if (files.length == 0) {
			throw new WrongParameterValueException("Given list of files for paramter \"" + getName()
					+ "\" is either empty or has the wrong format!\nParameter value required:\n" + getDescription());
		}

		if (fileType == FileParameter.FILE_IN) {
			for (String f : files) {
				File file = new File(f);
				try {
					if (!file.exists()) {

						throw new WrongParameterValueException("Given file " + file.getPath() + " for parameter \"" + getName()
								+ "\" does not exist!\n");
					}
				}

				catch (SecurityException e) {
					throw new WrongParameterValueException("Given file \"" + file.getPath() + "\" cannot be read, access denied!\n"
							+ e.getMessage());
				}
			}
		}
		return true;
	}
}
