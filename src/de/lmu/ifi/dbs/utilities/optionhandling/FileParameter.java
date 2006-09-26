package de.lmu.ifi.dbs.utilities.optionhandling;

import java.io.File;

public class FileParameter extends Parameter<File> {

	public static int FILE_IN = 1;

	public static int FILE_OUT = 2;

	private int fileType;

	public FileParameter(String name, String description, String directory, int fileType) {
		super(name, description);

		this.fileType = fileType;

	}

	public FileParameter(String name, String description, int fileType) {
		this(name, description, null, fileType);
	}

	@Override
	public String getValue() {
		return value.getPath();
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = new File(value);
		}
	}

	public boolean isValid(String value) throws ParameterException {

		if (value == null) {
			throw new WrongParameterValueException("Parameter \"" + getName()
					+ "\": No filename given!\nParameter description: " + getDescription());
		}

		File file = new File(value);

		if (fileType == FILE_IN) {
			try {
				if (!file.exists()) {
					throw new WrongParameterValueException("Given file " + file.getPath()
							+ " for parameter \"" + getName() + "\" does not exist!\n");
				}
			} catch (SecurityException e) {
				throw new WrongParameterValueException("Given file \"" + file.getPath()
						+ "\" cannot be read, access denied!\n" + e.getMessage());
			}
		}
		return true;
	}

}
