package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class StringParameter extends Parameter<String> {

	public StringParameter(String name, String descripion) {
		super(name, descripion);

	}

	public StringParameter(String name, String description, ParameterConstraint con) {
		this(name, description);
		addConstraint(con);
	}

	public StringParameter(String name, String description, List<ParameterConstraint> cons) {
		this(name, description);
		addConstraintList(cons);
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
		
		if(isValid(value)){
			this.value = value;
		}

	}

	
	public boolean isValid(String value) throws ParameterException {
		
		try{
		for (ParameterConstraint<String> cons : this.constraints) {
			cons.test(value);
		}
		}
		catch(ParameterException ex){
			throw new WrongParameterValueException("Specified parameter value for parameter \""
					+ getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
		}
		
		return true;
		
	}
}
