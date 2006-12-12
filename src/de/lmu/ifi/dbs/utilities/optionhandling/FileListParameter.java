package de.lmu.ifi.dbs.utilities.optionhandling;

import java.io.File;
import java.util.List;
import java.util.Vector;

public class FileListParameter extends ListParameter<File> {

	private int fileType;

	public FileListParameter(String name, String description, int fileType) {
		super(name, description);
		this.fileType = fileType;
	}

	@Override
	public List<File> getValue() throws UnusedParameterException {
		if (value == null)
			throw new UnusedParameterException("Parameter " + name + " is not specified!");

		return value;
	}

	@Override
	public boolean isSet() {

		return (value != null);
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

	@Override
	public int getListSize() {
		return this.value.size();
	}

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
