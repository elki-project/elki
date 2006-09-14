package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class StringParameter extends Parameter<String> {

	
	public StringParameter(String name,String descripion){
		super(name,descripion);
		inputField = createInputField();
	}
	
	public StringParameter(String name, String description, ParameterConstraint con){
		this(name,description);
		addConstraint(con);
	}
	
	public StringParameter(String name, String description, List<ParameterConstraint> cons){
		this(name,description);
		addConstraintList(cons);
	}
	
	private JComponent createInputField(){
		
		JTextField field = new JTextField();
		field.setColumns(20);
		return field;
	}
	
	public JComponent getInputField() {
		return inputField;
	}


	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {
		if(!this.constraints.isEmpty()){
			for(ParameterConstraint<String> cons : this.constraints){
				cons.test(value);
			}
		}
		this.value = value;	
	}


	@Override
	public void setValue() throws ParameterException {
		setValue(((JTextField)inputField).getText());
		
	}

}
