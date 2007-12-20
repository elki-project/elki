package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Equal-Than-Number parameter constraint. The value of the
 * number parameter ({@link NumberParameter}) tested has to be less equal than the specified constraint
 * value.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessEqualConstraint extends AbstractLoggable implements ParameterConstraint<Number> {

	/**
	 * Parameter constraint value.
	 */
	private Number constraintNumber;

	/**
	 * Creates a Less-Equal-Than-Number parameter constraint.
	 * 
	 * That is, the value of
	 * the appropriate number parameter has to be less equal than the given constraint value.
	 * 
	 * @param constraintNumber
	 *            parameter constraint value
	 */
	public LessEqualConstraint(Number constraintNumber) {
        super(LoggingConfiguration.DEBUG);
		this.constraintNumber = constraintNumber;
	}

	/**
	 * Checks if the number value given by the number parameter is less equal than the parameter constraint
	 * value. If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {
		if (t.doubleValue() > constraintNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error: \n"
					+ "The parameter value specified has to be less equal than "
					+ constraintNumber.toString() + ". (current value: "+t.doubleValue()+")\n");
		}

	}

}
