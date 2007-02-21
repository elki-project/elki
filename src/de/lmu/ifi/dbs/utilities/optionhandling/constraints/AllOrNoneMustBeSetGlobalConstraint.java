package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Global parameter constraint specifying that either all elements of a list of
 * parameters ({@link de.lmu.ifi.dbs.utilities.optionhandling.Parameter}) must be set, or none of them.
 * 
 * @author Steffi Wanka
 * 
 */
public class AllOrNoneMustBeSetGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * List of parameters to be checked
	 */
	private List<Parameter> parameterList;

	/**
	 * Constructs a global parameter constraint for testing if either all
	 * elements of a list of parameters are set or none of them.
	 * 
	 * @param parameters
	 *            list of parameters to be checked
	 */
	public AllOrNoneMustBeSetGlobalConstraint(List<Parameter> parameters) {
		this.parameterList = parameters;
	}

	/**
	 * Checks if either all elements of a list of parameters are set, or none of
	 * them. If not, a parameter exception is thrown.
	 * 
	 * @see GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		Vector<String> set = new Vector<String>();
		Vector<String> notSet = new Vector<String>();

		for (Parameter p : parameterList) {
			if (p.isSet()) {
				set.add(p.getName());
			} else {
				notSet.add(p.getName());
			}
		}
		if (set.size() != 0 && notSet.size() != 0) {
			throw new WrongParameterValueException("Global Constraint Error.\n" + "Either all of the parameters " + paramsToString()
					+ " must be set or none of them. " + "Parameter(s) currently set: " + set.toString() + ", parameters currently "
					+ "not set: " + notSet.toString());
		}
	}

	/**
	 * Returns a list of names of the parameters.
	 * 
	 * @return the list of names of the parameters.
	 */
	private String paramsToString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		int counter = 1;
		for (Parameter p : parameterList) {
			buffer.append(p.getName());
			if (counter != parameterList.size()) {
				buffer.append(",");
			}
			counter++;
		}
		buffer.append("]");
		return buffer.toString();
	}

}
