package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Parameter class for a parameter specifying a class name.
 * 
 * @author Steffi Wanka
 * 
 */
public class ClassParameter extends Parameter<String, String> {

	/**
	 * the restriction class for this class parameter.
	 */
	private Class restrictionClass;

	/**
	 * Constructs a class parameter with the given name, description, and
	 * restriction class.
	 * 
	 * @param name
	 *            the parameter name
	 * @param description
	 *            the parameter description
	 * @param restrictionClass
	 *            the restriction class of this class parameter
	 */
	public ClassParameter(String name, String description,
			Class<?> restrictionClass) {
		super(name, description);
		this.restrictionClass = restrictionClass;
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = value;
		}
	}

	/**
	 * Returns the class names allowed according to the restriction class of
	 * this class parameter.
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

	/**
	 * Checks if the given parameter value is valid for this ClassParameter. If
	 * not a parameter exception is thrown.
	 * 
	 */
	public boolean isValid(String value) throws ParameterException {

		if (value == null) {
			throw new WrongParameterValueException(
					"Parameter Error.\nNo value for parameter \"" + getName()
							+ "\" " + "given.");
		}

		try {
			Util.instantiate(restrictionClass, value);

		}

		catch (UnableToComplyException e) {
			throw new WrongParameterValueException(this.name, value,"subclass of " + restrictionClass.getName(), e);
		}

		return true;
	}

	/**
	 * Returns the restriction class of this class parameter.
	 * 
	 * @return the restriction class of this class parameter.
	 */
	public Class getRestrictionClass() {
		return restrictionClass;
	}
}