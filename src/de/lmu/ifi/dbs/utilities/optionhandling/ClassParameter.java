package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

public class ClassParameter extends Parameter<String> {

	private Class restrictionClass;
	
	private Parameterizable parameterizableValue;

	public ClassParameter(String name, String description, Class restrictionClass) {
		super(name, description);
		this.restrictionClass = restrictionClass;
	}

	public ClassParameter(String name, String description, Class restrictionClass,
			List<ParameterConstraint> constraints) {
		this(name, description, restrictionClass);
		addConstraintList(constraints);
	}

	public ClassParameter(String name, String description, Class restrictionClass,
			ParameterConstraint constraint) {
		this(name, description, restrictionClass);
		addConstraint(constraint);
	}

	public ClassParameter(String name, String description, String defaultValue,
			Class restrictionClass) {
		this(name, description, restrictionClass);
		this.defaultValue = defaultValue;
	}

	@Override
	public String getValue() throws UnusedParameterException {
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
			this.value = value;
		}
	}

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

		if(value == null){
			throw new WrongParameterValueException("Parameter Error!!\nNo value for parameter \""+getName()+"\" " +
					"given!");
		}
		
		try {
			Util.instantiate(restrictionClass, value);

		}
		
		catch (UnableToComplyException e) {
			throw new WrongParameterValueException("Wrong parameter value for parameter +\""
					+ getName() + "\". Given class " + value
					+ " does not extend restriction class " + restrictionClass + "!\n");
		}

		return true;
	}
	
	public Class getRestrictionClass(){
		return restrictionClass;
	}
}