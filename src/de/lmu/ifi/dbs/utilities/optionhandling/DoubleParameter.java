package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class DoubleParameter extends NumberParameter<Double> {

	public DoubleParameter(String name, String description) {
		super(name, description);

	}

	public DoubleParameter(String name, String description, ParameterConstraint cons) {
		this(name, description);
		addConstraint(cons);
	}

	public DoubleParameter(String name, String description, List<ParameterConstraint> cons) {
		this(name, description);
		addConstraintList(cons);
	}

	@Override
	public String getValue() {
		return this.value.toString();
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = Double.parseDouble(value);
		}
	}

	public Number getNumberValue() {
		return value;
	}

	public boolean isValid(String value) throws ParameterException {
		try {
			Double.parseDouble(value);
		}

		catch (NumberFormatException e) {
			throw new WrongParameterValueException("Wrong parameter format! Parameter \""
					+ getName() + "\" requires a double value, read: " + value + "!\n");
		}

		try {
			for (ParameterConstraint<Number> cons : this.constraints) {

				cons.test(Double.parseDouble(value));
			}
		} catch (ParameterException ex) {
			throw new WrongParameterValueException("Specified parameter value for parameter \""
					+ getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
		}

		return true;
	}
}
