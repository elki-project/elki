package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Global parameter constraint specifying that only one parameter of a list of parameters
 * are allowed to be set.
 * 
 * @author Steffi Wanka
 *
 */
public class OnlyOneIsAllowedToBeSetGlobalConstraint implements
		GlobalParameterConstraint {

	/**
	 * List of parameters to be checked.
	 */
	private List<Parameter> parameters;
	
	/**
	 * Constructs a global parameter constraint for testing if only one parameter of a list
	 * of parameters is set.
	 * 
	 * @param params list of parameters to be checked
	 */
	public OnlyOneIsAllowedToBeSetGlobalConstraint(List<Parameter> params){
		parameters = params;
	}
	
	/**
	 * Checks if only one parameter of a list of parameters is set. If not, a parameter exception
	 * is thrown.
	 * 
	 */
	public void test() throws ParameterException {
		
		boolean oneIsSet = false;

		
		for(Parameter p : parameters){
			
			if(!oneIsSet){
				oneIsSet = p.isSet();
			}
			else if(oneIsSet && p.isSet()){
				throw new WrongParameterValueException("Global Parameter Constraint Error!\n" +
						"Only one of the parameters "+constraintsToString()+" is allowed to be set!");
			}
		}
	}
	
	private String constraintsToString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		for(int i = 0; i < parameters.size(); i++){
			buffer.append(parameters.get(i).getName());
			if(i != parameters.size()-1){
				buffer.append(",");
			}
		}
		buffer.append("]");
		return buffer.toString();
	}

}
