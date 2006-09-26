package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.Vector;

public class DoubleListParameter extends ListParameter<Double> {

	public DoubleListParameter(String name, String description) {
		super(name, description);

	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

	@Override
	public String getValue() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < value.size(); i++) {
			buffer.append(value.get(i));
			if (i != value.size() - 1) {
				buffer.append(SPLIT);
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
			String[] values = SPLIT.split(value);
			Vector<Double> doubleValue = new Vector<Double>();
			for (String val : values) {
				doubleValue.add(Double.parseDouble(val));
			}
			this.value = doubleValue;
		}
	}

	public void setDefaultValue(double defaultValue) {

	}

	public boolean isValid(String value) throws ParameterException {

		String[] values = SPLIT.split(value);
		if (values.length == 0) {

			throw new WrongParameterValueException(
					"Wrong parameter format! Given list of double values for parameter \""
							+ getName()
							+ "\" is either empty or has the wrong format!\nParameter value required:\n"
							+ getDescription());
		}

		for (String val : values) {

			try {
				Double.parseDouble(val);
			} catch (NumberFormatException e) {
				throw new WrongParameterValueException("Wrong parameter format for parameter \""
						+ getName() + "\". Given parameter " + val + " is no double!\n");
			}

		}

		for (ParameterConstraint<Number> cons : this.constraints) {

			for (String val : values) {
				try {
					cons.test(Double.parseDouble(val));
				} catch (ParameterException ex) {
					throw new WrongParameterValueException(
							"Specified parameter "+val+" for parameter \"" + getName()
									+ "\" breaches parameter constraint!\n" + ex.getMessage());
				}
			}
		}

		return true;
	}
}
