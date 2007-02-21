package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * <p>Interface for specifying global parameter constraints, i.e. constraints addressing
 * several parameters.
 * </p>
 * <p>Each class specifying a global parameter constraint should implement this interface.
 * The parameters to be tested should be defined as private attributes and should be
 * initialized in the respective constructor of the class, i.e. they are parameters of the constructor.
 * The proper constraint test should be implemented in the method {@link #test()}. 
 * </p>
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
	public abstract void test() throws ParameterException;
	
}
