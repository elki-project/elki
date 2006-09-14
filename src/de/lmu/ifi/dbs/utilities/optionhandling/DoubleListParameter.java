package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.Component;
import java.util.Vector;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class DoubleListParameter extends ListParameter<Double> {

	public DoubleListParameter(String name, String description) {
		super(name, description);
		inputField = createInputField();
	}

	private JComponent createInputField() {
		JTextField field = new JTextField();
		field.setColumns(30);
		field.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent comp) {

				String[] values = SPLIT.split(((JTextField) inputField)
						.getText());
				for (String val : values) {

					try {
						Double d = Double.parseDouble(val);

						for (ParameterConstraint<Number> con : constraints) {
							con.test(d);
						}
					} catch (NumberFormatException e) {
						return false;
					}
					catch(ParameterException e){
						return false;
					}
				}

				return true;
			}
		});

		return field;
	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

	@Override
	public Component getInputField() {
		return inputField;
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

		String[] values = SPLIT.split(value);
		if (values.length == 0) {
			// TODO
			throw new WrongParameterValueException("");
		}
		Vector<Double> doubleValue = new Vector<Double>();
		for (String val : values) {

			try {
				Double.parseDouble(val);
			}
			// TODO
			catch (NumberFormatException e) {
				throw new WrongParameterValueException("");
			}
			doubleValue.add(Double.parseDouble(val));
		}

		// check possible constraints

		for (ParameterConstraint<Number> cons : this.constraints) {
			for (Double d : doubleValue) {
				cons.test(d);
			}
		}

		this.value = doubleValue;
	}

	@Override
	public void setValue() throws ParameterException {
		setValue(((JTextField) inputField).getText());

	}

	public void setDefaultValue(double defaultValue){
		
	}
}
