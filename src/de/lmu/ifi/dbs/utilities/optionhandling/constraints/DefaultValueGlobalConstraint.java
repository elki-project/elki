package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Global parameter constraint for specifying the default value of a parameter dependent on
 * the parameter value of another parameter.
 * 
 * @author Steffi Wanka
 *
 */
public class DefaultValueGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * Parameter to be set.
	 */
	private Parameter needsValue;
	
	/**
	 * Parameter providing the value.
	 */
	private Parameter hasValue;
	
	/**
	 * Creates a global parameter constraint for specifying the default value of a parameter
	 * dependent on the value of an another paramter.
	 * 
	 * @param needsValue the parameter which default value is to be set
	 * @param hasValue the parameter providing the value
	 */
	public DefaultValueGlobalConstraint(Parameter needsValue, Parameter hasValue ){
		this.needsValue = needsValue;
		this.hasValue = hasValue;
	}
	
	/**
	 * Checks if the parameter providing the default value is already set, 
	 * and if the two parameter are of the same parameter type. If not, 
	 * a parameter exception is thrown. 
	 */
	public void test() throws ParameterException {
		

		if(!this.hasValue.isSet()){
			throw new WrongParameterValueException("Parameter "+hasValue.getName()+" is not set but has to be!");
		}

		if(!needsValue.getClass().equals(hasValue.getClass())){
			throw new WrongParameterValueException("Global Parameter Constraint Error!\n" +
					"Parameters "+hasValue.getName()+" and "+needsValue.getName()+"" +
							" must be of the same parameter type!");
		}
		
		if(!needsValue.isSet()){
			needsValue.setDefaultValue(hasValue.getValue());
			needsValue.setDefaultValueToValue();
		}
	}

}
