package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

/**
 * Represents a GlobalParameterConstraints which demands that at least one
 * parameter value of a given list of parameters has to be set.
 * 
 * @author Steffi Wanka
 * 
 */
public class OneMustBeSetGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * a list of parameters
	 */
	private List<Parameter> parameters;

	/**
	 * Creates a GlobalParameterConstraint which demands that at least one
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
	 * specified is set. If not a Parameter Exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i).isSet()) {
				return;
			}
			if (!parameters.get(i).isSet() && i == parameters.size() - 1) {
				throw new WrongParameterValueException("Global Parameter Constraint Error!\n"
						+ "At least one value of the parameters " + parameterNames()
						+ "has to be set!");
			}
		}
	}
}
