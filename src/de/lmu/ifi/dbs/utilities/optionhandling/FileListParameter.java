package de.lmu.ifi.dbs.utilities.optionhandling;

import java.io.File;
import java.util.Vector;

public class FileListParameter extends ListParameter<File> {

	public FileListParameter(String name, String description) {
		super(name, description);

	}

	@Override
	public String getValue() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < value.size(); i++) {
			buffer.append(value.get(i));
			if (i != value.size() - 1) {
				buffer.append(",");
			}
		}
		return buffer.toString();
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
			throw new WrongParameterValueException(
					"Wrong parameter format! Given list of files for paramter \""
							+ getName()
							+ "\" is either empty or has the wrong format!\nParameter value required:\n"
							+ getDescription());
		}

		for (String f : files) {
			File file = new File(f);
			try {
				if (!file.exists()) {

					throw new WrongParameterValueException("Given file " + file.getPath()
							+ " for parameter \"" + getName() + "\" does not exist!\n");
				}
			}

			catch (SecurityException e) {
				throw new WrongParameterValueException("Given file \"" + file.getPath()
						+ "\" cannot be read, access denied!\n" + e.getMessage());
			}

		}
		return true;
	}
}
