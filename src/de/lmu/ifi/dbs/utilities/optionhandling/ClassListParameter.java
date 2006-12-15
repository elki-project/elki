package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.Arrays;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Parameter class for a parameter specifying a list of class names.
 * 
 * @author Steffi Wanka
 *
 */
public class ClassListParameter extends ListParameter<String> {

	/**
	 * the restriction class for the list of class names.
	 */
	private Class restrictionClass;

	/**
	 * Constructs a class list parameter with the given name, description, and restriction class.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param restrictionClass the restriction class of the list of class names
	 */
	public ClassListParameter(String name, String description, Class restrictionClass) {
		super(name, description);
		this.restrictionClass = restrictionClass;
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			String[] classes = SPLIT.split(value);
			this.value = Arrays.asList(classes);
		}
	}

	/**
	 * Returns the class names allowed according to the restriction class of this parameter.
	 * 
	 * @return class names allowed according to the restriction class defined.
	 */
	public String[] getRestrictionClasses() {
		if (restrictionClass != null) {
			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName
					.getOrCreatePropertyName(restrictionClass));
		}
		return new String[] {};
	}

	
	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
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
