package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a parameter constraint for testing if the value of the
 * number parameter ({@link NumberParameter}) tested is greater than the specified constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class GreaterConstraint extends AbstractLoggable implements ParameterConstraint<Number> {

	/**
	 * Parameter constraint value.
	 */
	private Number testNumber;

	/**
	 * Creates a Greater-Than-Number parameter constraint.
	 * 
	 * That is, the value of the number
	 * parameter has to be greater than the given constraint value.
	 * 
	 * @param testNumber
	 *            parameter constraint value
	 */
	public GreaterConstraint(Number testNumber) {
        super(LoggingConfiguration.DEBUG);
		this.testNumber = testNumber;
	}

	/**
	 * Checks if the number value given by the number parameter is greater than the specified constraint
	 * value. If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() <= testNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error:\n"
					+ "The parameter value specified has to be greater than "
					+ testNumber.toString() + ". (current value: "+t.doubleValue()+")\n");
		}
	}

}
