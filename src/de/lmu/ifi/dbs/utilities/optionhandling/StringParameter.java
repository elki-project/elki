package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a string value.
 * 
 * @author Steffi Wanka
 *
 */
public class StringParameter extends Parameter<String,String> {

	/**
	 * Constructs a string parameter with the given name and description
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public StringParameter(String name, String description) {
		super(name, description);

	}

	/**
	 * Constructs a string parameter with the given name, description, and a parameter constraint.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param con a parameter constraint for this string parameter
	 */
	public StringParameter(String name, String description, ParameterConstraint<String> con) {
		this(name, description);
		addConstraint(con);
	}

	/**
	 * Constructs a string parameter with the given name, description, and a list of parameter constraints.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param cons a list of parameter constraints for this string parameter
	 */
	public StringParameter(String name, String description, List<ParameterConstraint<String>> cons) {
		this(name, description);
		addConstraintList(cons);
	}


	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
	 */
	public void setValue(String value) throws ParameterException {
		
		if(isValid(value)){
			this.value = value;
		}

	}

	
	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
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
