package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * <p>
 * Interface for specifying parameter constraints.
 * </p>
 * <p>
 * Each class specifying a constraint addressing only one parameter should implement this interface.
 * The constraint value for testing the parameter should be defined as private attribute and should be initialized in the
 * respective constructor of the class, i.e. it is a parameter of the constructor. The proper constraint
 * test should be implemented in the method {@link #test(Object) test(T)}.
 * </p>
 * @author Steffi Wanka
 * 
 * @param <T>
 */
public interface ParameterConstraint<T> {

	/**
	 * Checks if the value {@code t} of the parameter to be tested fulfills the parameter constraint.
	 * If not, a parameter exception is thrown.
	 * 
	 * @param t
	 *            Value to be checked whether or not it fulfills the underlying
	 *            parameter constraint.
     * @throws ParameterException
	 */
	public abstract void test(T t) throws ParameterException;

}
