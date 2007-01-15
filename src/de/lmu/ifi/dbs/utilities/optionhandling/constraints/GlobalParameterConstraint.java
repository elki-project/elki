package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Interface for specifying global parameter constraints, i.e. constraints affecting
 * several parameters.
 * 
 * @author Steffi Wanka
 *
 */
public interface GlobalParameterConstraint {

	/**
	 * Checks if the respective parameters satisfy the parameter constraint. If not,
	 * a parameter exception is thrown.
	 * 
	 * @throws ParameterException if the parameters don't satisfy the parameter constraint.
	 */
	void test() throws ParameterException;
}
