package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Global parameter constraint specifying that only one parameter of a list of
 * parameters ({@link Parameter}) is allowed to be set.
 * 
 * @author Steffi Wanka
 * 
 */
public class OnlyOneIsAllowedToBeSetGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * List of parameters to be checked.
	 */
	private List<Parameter<?,?>> parameters;

	/**
	 * Constructs a global parameter constraint for testing if only one
	 * parameter of a list of parameters is set.
	 * 
	 * @param params
	 *            list of parameters to be checked
	 */
	public OnlyOneIsAllowedToBeSetGlobalConstraint(List<Parameter<?,?>> params) {
		parameters = params;
	}

	/**
	 * Checks if only one parameter of a list of parameters is set. If not, a
	 * parameter exception is thrown.
	 * 
	 * @see GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		Vector<String> set = new Vector<String>();
		Vector<String> notSet = new Vector<String>();

		for (Parameter<?,?> p : parameters) {

			if (p.isSet()) {

				set.add(p.getName());
			} else {
				notSet.add(p.getName());
			}

		}
		if (set.size() > 1) {
			throw new WrongParameterValueException("Global Parameter Constraint Error.\n" + "Only one of the parameters "
					+ constraintsToString() + " is allowed to be set. " + "Parameters currently set: " + set.toString());
		}
	}

	private String constraintsToString() {
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

}
