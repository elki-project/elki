package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;
import java.util.Vector;

public class DoubleListParameter extends ListParameter<Double> {

	public DoubleListParameter(String name, String description) {
		super(name, description);

	}

	public DoubleListParameter(String name, String description, ParameterConstraint<ListParameter> con) {
		this(name, description);
		addConstraint(con);
	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

	@Override
	public List<Double> getValue() throws UnusedParameterException {
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
			String[] values = SPLIT.split(value);
			Vector<Double> doubleValue = new Vector<Double>();
			for (String val : values) {
				doubleValue.add(Double.parseDouble(val));
			}
			this.value = doubleValue;
		}
	}

	public boolean isValid(String value) throws ParameterException {

		String[] values = SPLIT.split(value);
		if (values.length == 0) {

			throw new WrongParameterValueException("Wrong parameter format! Given list of double values for parameter \"" + getName()
					+ "\" is either empty or has the wrong format!\nParameter value required:\n" + getDescription());
		}

		for (String val : values) {

			try {
				Double.parseDouble(val);
			} catch (NumberFormatException e) {
				throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given parameter " + val
						+ " is no double!\n");
			}

		}

		for (ParameterConstraint<ListParameter> cons : this.constraints) {

			cons.test(this);
		}

		return true;
	}
}
