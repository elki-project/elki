package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Interface for specifying parameter constraints.
 * 
 * @author Steffi Wanka
 * 
 * @param <T>
 */
public interface ParameterConstraint<T> {

	/**
	 * Checks if the respective parameter fulfills the parameter constraint. If
	 * not a parameter exception is thrown.
	 * 
	 * @param t  Value to be checked whether or not it fulfills the underlying parameter constraint. 
	 * @throws ParameterException
	 */
	void test(T t) throws ParameterException;
}
