package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Global parameter constraint describing the dependency of a parameter on a given flag. 
 * Depending on the status of a given flag the parameter is tested for its constraints or not.   
 * 
 * @author Steffi Wanka
 *
 */
public class ParameterFlagGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * Parameter possibly to be checked
	 */
	private Parameter param;

	/**
	 * Flag the checking of the parameter constraints is dependent on
	 */
	private Flag flag;

	/**
	 * Indicates at which status of the flag the parameter is to be checked
	 */
	private boolean flagConstraint;

	/**
	 * List of parameter constraints 
	 */
	private List<ParameterConstraint> cons;

	/**
	 * Constructs a global parameter constraint describing that the testing of a gvien parameter
	 * is dependent on the state of a given flag. 
	 * 
	 * @param p Parameter possible to be checked
	 * @param c A list of parameter constraints
	 * @param f Flag controlling the checking of the parameter constraints
	 * @param flagConstraint Indicates at which status of the flag the parameter is to be checked
	 */
	public ParameterFlagGlobalConstraint(Parameter p, List<ParameterConstraint> c, Flag f, boolean flagConstraint) {
		param = p;
		flag = f;
		this.flagConstraint = flagConstraint;
		cons = c;
	}

	/**
	 * Checks the parameter for its parameter constraints only dependently on the status 
	 * the given flag. If a parameter constraint is breached a parameter exception is thrown.
	 */
	public void test() throws ParameterException {

		if (flagConstraint) {
			// only check constraints of param if flag is set
			if (flag.isSet()) {

				for (ParameterConstraint c : cons) {
					c.test(param.getValue());
					
				}
			}
		} else {
			if (!flag.isSet()) {
				for (ParameterConstraint c : cons) {
					c.test(param.getValue());
				}
			}
		}
	}

}
