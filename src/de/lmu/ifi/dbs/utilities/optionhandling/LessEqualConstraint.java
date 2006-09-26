package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Represents a Less-Equal-Than-Number ParameterConstraint. The value of the
 * parameter to be tested has to be less equal than the specified constraint
 * value.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessEqualConstraint implements ParameterConstraint<Number> {

	/**
	 * parameter constraint value
	 */
	private Number testNumber;

	/**
	 * Creates a Less-Equal-Than-Number ParameterConstraint, i.e. the value of
	 * the parameter has to be less equal than the given constraint number.
	 * 
	 * @param testNumber
	 *            parameter constraint value
	 */
	public LessEqualConstraint(Number testNumber) {
		this.testNumber = testNumber;
	}

	/**
	 * Checks if the given number is less equal than the parameter constraint
	 * value. If not a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {
		if (t.doubleValue() > testNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error: \n"
					+ "The parameter value specified has to be less equal than "
					+ testNumber.toString() + ".\n");
		}

	}

}
