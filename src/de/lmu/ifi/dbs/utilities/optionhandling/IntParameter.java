package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class IntParameter extends NumberParameter<Integer> {

	public IntParameter(String name, String description) {
		super(name, description);

		inputField = createInputField();
	}

	public IntParameter(String name, String description,
			ParameterConstraint constraint) {
		this(name, description);
		addConstraint(constraint);
	}

	public IntParameter(String name, String description,
			List<ParameterConstraint> constraints) {
		this(name, description);
		addConstraintList(constraints);
	}

	private JComponent createInputField() {

		// JFormattedTextField field = new
		// JFormattedTextField(NumberFormat.getIntegerInstance());
		JTextField field = new JTextField();
		// field.setInputVerifier(new IntegerVerifier());
		field.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				JTextField tf = (JTextField) input;
				String text = tf.getText();
				if(text.equals("") ){
					return true;
				}
				try {
					Integer.parseInt(text);
				} catch (NumberFormatException e) {
					System.out.println(inputField.getTopLevelAncestor().getClass().getName());
//					passMessage(e);
//					new ErrorDialog((JFrame)inputField.getTopLevelAncestor(),"",e);
					return false;
				}
				// check possible constraints
				for (ParameterConstraint<Number> con : constraints) {

					try {
						con.test(Integer.parseInt(text));
					} catch (ParameterException e) {
						return false;
					}
				}
				return true;
			}
		});
		field.setColumns(5);

		// field.addPropertyChangeListener(new PropertyChangeListener(){
		//			
		// public void propertyChange(PropertyChangeEvent e){
		// System.out.println(e.getNewValue());
		// }
		// });
		//		
		return field;
	}

	public JComponent getInputField() {

		return inputField;
	}

	@Override
	public String getValue() {
		return value.toString();
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		try {
			Integer.parseInt(value);
		}
		// TODO
		catch (NumberFormatException e) {
			throw new WrongParameterValueException("");
		}

		for (ParameterConstraint<Number> cons : this.constraints) {
			cons.test(Integer.parseInt(value));
		}

		this.value = Integer.parseInt(value);
	}

	public void setValue() throws ParameterException {
		setValue(((JTextField) inputField).getText());

	}
	
	private void passMessage(Exception e) throws ParameterException{
		throw new WrongParameterValueException("");
		
	}

	public Number getNumberValue(){
		return value;
	}
}
