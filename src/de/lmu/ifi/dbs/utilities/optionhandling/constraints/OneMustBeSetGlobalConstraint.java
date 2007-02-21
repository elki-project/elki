package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a global parameter constraint specifying that at least one
 * parameter value of a given list of parameters ({@link Parameter}) has to be set.
 * 
 * @author Steffi Wanka
 * 
 */
public class OneMustBeSetGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * List of parameters to be checked.
	 */
	private List<Parameter> parameters;

	/**
	 * Creates a One-Must-Be-Set global parameter constraint.
	 * That is, at least one
	 * parameter value of the given list of parameters has to be set.
	 * 
	 * @param params
	 *            list of parameters
	 */
	public OneMustBeSetGlobalConstraint(List<Parameter> params) {
		parameters = params;
	}

	private String parameterNames() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		for (int i = 0; i < parameters.size(); i++) {
			buffer.append(parameters.get(i).getName());
			if (i != parameters.size() - 1) {
				buffer.append(",");
			}
		}
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Checks if at least one parameter value of the list of parameters
	 * specified is set. If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		for(Parameter p : parameters){
			if(p.isSet())
				return;
		
		throw new WrongParameterValueException("Global Parameter Constraint Error.\n"
				+ "At least one of the parameters " + parameterNames()
				+ " has to be set.");
	}
	}
}
