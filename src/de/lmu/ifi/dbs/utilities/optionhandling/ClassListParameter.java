package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.Arrays;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

public class ClassListParameter extends ListParameter<String> {

	private Class restrictionClass;

	public ClassListParameter(String name, String description, Class restrictionClass) {
		super(name, description);
		this.restrictionClass = restrictionClass;

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
			String[] classes = SPLIT.split(value);
			this.value = Arrays.asList(classes);
		}
	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

	public String[] getRestrictionClasses() {
		if (restrictionClass != null) {
			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName
					.getOrCreatePropertyName(restrictionClass));
		}
		return new String[] {};
	}

	/**
	 * Checks if the given parameter value is a valid value for this
	 * ClassListParameter. If not a parameter exception is thrown.
	 */
	public boolean isValid(String value) throws ParameterException {

		String[] classes = SPLIT.split(value);
		if (classes.length == 0) {
			throw new WrongParameterValueException(
					"Wrong parameter format! Given list of classes for paramter \""
							+ getName()
							+ "\" is either empty or has the wrong format!\nParameter value required:\n"
							+ getDescription());
		}
		for (String cl : classes) {
			try {
				Util.instantiate(restrictionClass, cl);
			} catch (UnableToComplyException e) {
				throw new WrongParameterValueException("Wrong parameter value for parameter +\""
						+ getName() + "\". Given class " + cl
						+ " does not extend restriction class " + restrictionClass + "!\n");
			}
		}

		return true;
	}

}
