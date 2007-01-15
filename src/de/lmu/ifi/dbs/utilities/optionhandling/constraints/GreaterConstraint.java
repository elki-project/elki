package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Greater-Than-Number ParameterConstraint. The (number) value of the
 * parameter tested has to be greater than the specified constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class GreaterConstraint implements ParameterConstraint<Number> {

	/**
	 * parameter constraint value
	 */
	private Number testNumber;

	/**
	 * Creates a Greater-Than-Number ParameterConstraint, i.e. the value of the
	 * parameter has to be greater than the given constraint value.
	 * 
	 * @param testNumber
	 *            parameter constraint value
	 */
	public GreaterConstraint(Number testNumber) {
		this.testNumber = testNumber;
	}

	/**
	 * Checks if the given number is greater than the parameter constraint
	 * value. If not a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() <= testNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error:\n"
					+ "The parameter value specified has to be greater than "
					+ testNumber.toString() + ".\n");
		}
	}

}
