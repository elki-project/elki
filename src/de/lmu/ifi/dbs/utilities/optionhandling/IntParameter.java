package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class IntParameter extends NumberParameter<Integer> {

	public IntParameter(String name, String description) {
		super(name, description);

	}

	public IntParameter(String name, String description, ParameterConstraint constraint) {
		this(name, description);
		addConstraint(constraint);
	}

	public IntParameter(String name, String description, List<ParameterConstraint> constraints) {
		this(name, description);
		addConstraintList(constraints);
	}

	@Override
	public String getValue() throws UnusedParameterException {
    if (value == null)
      throw new UnusedParameterException("Parameter " + name + " is not specified!");

    return value.toString();
  }

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = Integer.parseInt(value);
		}
	}

	public Number getNumberValue() {
		return value;
	}

	/**
	 * Checks if the given value is a valid integer value for this IntParameter.
	 * If not a ParameterException is thrown.
	 * 
	 */
	public boolean isValid(String value) throws ParameterException {

		try {
			Integer.parseInt(value);

		} catch (NumberFormatException e) {
			throw new WrongParameterValueException("Wrong parameter format! Parameter \""
					+ getName() + "\" requires an integer value!\n");
		}

		try {
			for (ParameterConstraint<Number> cons : this.constraints) {
				cons.test(Integer.parseInt(value));
			}
		} catch (ParameterException ex) {
			throw new WrongParameterValueException("Specified parameter value for parameter \""
					+ getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
		}
		return true;
	}

}
