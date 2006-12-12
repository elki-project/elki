package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class LongParameter extends NumberParameter<Long> {

	public LongParameter(String name, String description) {
		super(name, description);
	}

	public LongParameter(String name, String description, ParameterConstraint cons) {
		this(name, description);
		addConstraint(cons);
	}

	public LongParameter(String name, String description, List<ParameterConstraint> cons) {
		this(name, description);
		addConstraintList(cons);
	}

	@Override
	public Number getNumberValue() {
		return value;
	}

	@Override
	public Long getValue() throws UnusedParameterException {
		if (value == null)
			throw new UnusedParameterException("Parameter " + name + " is not specified!");

		return value;
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public boolean isValid(String value) throws ParameterException {
		try {
			Long.parseLong(value);
		}

		catch (NumberFormatException e) {
			throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long value, read: "
					+ value + "!\n");
		}

		try {
			for (ParameterConstraint<Number> cons : this.constraints) {

				cons.test(Long.parseLong(value));
			}
		} catch (ParameterException ex) {
			throw new WrongParameterValueException("Specified parameter value for parameter \"" + getName()
					+ "\" breaches parameter constraint!\n" + ex.getMessage());
		}

		return true;
	}

	@Override
	public void setValue(String value) throws ParameterException {
		if (isValid(value)) {
			this.value = Long.parseLong(value);
		}
	}

}
