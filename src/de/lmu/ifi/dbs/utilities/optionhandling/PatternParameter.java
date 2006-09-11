package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.Component;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.distance.DistanceFunction;

public class PatternParameter extends Parameter<String> {

	private Class patternClass;

	public PatternParameter(String name, String description) {
		super(name, description);
		inputField = createInputField();

	}

	public PatternParameter(String name, String description, Class patternClass) {
		super(name, description);
		this.patternClass = patternClass;
	}

	private JComponent createInputField() {
		JTextField field = new JTextField();
		field.setInputVerifier(new InputVerifier() {

			public boolean verify(JComponent comp) {

				String text = ((JTextField) comp).getText();
				try {
					Pattern.compile(text);
				} catch (PatternSyntaxException e) {
					return false;
				}
				return true;
			}
		});

		field.setColumns(30);
		return field;
	}

	@Override
	public Component getInputField() {
		return inputField;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {
		
		// test if value is a valid regular expression
		try{
			Pattern.compile(value);
		}
		catch(PatternSyntaxException e){
			throw new WrongParameterValueException("");
		}
		// test pattern class (if existent)
		if(this.patternClass != null){
			// create instance
			try {
				DistanceFunction obj = (DistanceFunction)patternClass.newInstance();
				obj.valueOf(value);
			} catch (InstantiationException e) {
				// TODO
				throw new WrongParameterValueException("");
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				throw new WrongParameterValueException("");
			}
			catch(IllegalArgumentException e){
//				 TODO Auto-generated catch block
				throw new WrongParameterValueException("");
			}
		}
		this.value = value;

	}

	@Override
	public void setValue() throws ParameterException {
		setValue(((JTextField)inputField).getText());

	}

}
