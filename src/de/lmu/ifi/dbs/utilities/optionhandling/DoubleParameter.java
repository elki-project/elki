package de.lmu.ifi.dbs.utilities.optionhandling;

import java.text.NumberFormat;
import java.util.List;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.DefaultFormatter;

public class DoubleParameter extends NumberParameter<Double> {

	public DoubleParameter(String name, String description) {
		super(name, description);

		inputField = createInputField();
	}

	public DoubleParameter(String name, String description,
			ParameterConstraint cons) {
		this(name, description);
		addConstraint(cons);
	}

	public DoubleParameter(String name, String description,
			List<ParameterConstraint> cons) {
		this(name, description);
		addConstraintList(cons);
	}

	private JComponent createInputField() {

//		 JFormattedTextField field = new JFormattedTextField(new
//		 DecimalFormat());
		NumberFormat f;
		JTextField field = new JTextField();
		field.setInputVerifier(new InputVerifier(){
			public boolean verify(JComponent input) {
				JTextField tf = (JTextField) input;
				String text = tf.getText();
				if(text.equals("")){
					return true;
				}
				try {
					Double.parseDouble(text);
				} catch (NumberFormatException e) {
					System.out.println("WRRRRONNNNGGGG!!!!");
					return false;
				}
				// check possible constraints
				for(ParameterConstraint<Number> con : constraints){
					try{
						con.test(Double.parseDouble(text));
					}
					catch(ParameterException e){
						return false;
					}
				}
				return true;
			}
		});
		new DefaultFormatter();
		field.setColumns(10);
		return field;
	}

	public JComponent getInputField() {
		return inputField;
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
		try {
			Double.parseDouble(value);
		}
		// TODO
		catch (NumberFormatException e) {
			throw new WrongParameterValueException("");
		}

		//check possible constraints
		for (ParameterConstraint<Number> cons : this.constraints) {
			cons.test(Double.parseDouble(value));
		}

		this.value = Double.parseDouble(value);

	}

	@Override
	public void setValue() throws ParameterException {

		setValue(((JTextField) inputField).getText());

	}
	
	public Number getNumberValue(){
		return value;
	}
}
