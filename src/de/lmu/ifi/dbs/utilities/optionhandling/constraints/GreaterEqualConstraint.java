package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Greater-Equal-Than-Number parameter constraint. The
 * value of the number parameter ({@link NumberParameter}) tested has to be greater equal than the specified
 * constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class GreaterEqualConstraint extends AbstractLoggable implements ParameterConstraint<Number> {

	/**
	 * Parameter constraint value.
	 */
	private Number constraintNumber;

	/**
	 * Creates a Greater-Equal parameter constraint.
	 * 
	 * That is, the value of the number
	 * parameter given has to be greater equal than the constraint value given.
	 * 
	 * @param constraintNumber
	 *            constraint parameter value
	 */
	public GreaterEqualConstraint(Number constraintNumber) {
        super(LoggingConfiguration.DEBUG);
		this.constraintNumber = constraintNumber;
	}

	/**
	 * Checks if the number value given by the number parameter is greater equal than the constraint
	 * value. If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() < constraintNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error: \n"
					+ "The parameter value specified has to be greater equal than "
					+ constraintNumber.toString() + ". (current value: "+t.doubleValue()+")\n");
		}
	}

}
