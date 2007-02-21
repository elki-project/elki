package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Than-Number parameter constraint. The value of the
 * number parameter ({@link NumberParameter}) tested has to be less than the specified constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessConstraint implements ParameterConstraint<Number> {

	/**
	 * Parameter constraint value.
	 */
	private Number constraintNumber;

	/**
	 * Creates a Less-Than-Number parameter constraint.
	 * 
	 * That is, the value of the number
	 * parameter tested has to be less than the constraint value given.
	 * 
	 * @param constraintNumber
	 *            parameter constraint value
	 */
	public LessConstraint(Number constraintNumber) {
		this.constraintNumber = constraintNumber;
	}

	/**
	 * Checks if the number value given by the number parameter is less than the parameter constraint value.
	 * If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() >= constraintNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error: \n"
					+ "The parameter value specified has to be less than " + constraintNumber.toString()
					+ ".\n");
		}
	}

}
