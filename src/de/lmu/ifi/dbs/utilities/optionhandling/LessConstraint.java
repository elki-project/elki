package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Represents a Less-Than-Number ParameterConstraint. The (number) value of the
 * parameter to be tested has to less than the specified constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessConstraint implements ParameterConstraint<Number> {

	/**
	 * parameter constraint value
	 */
	private Number testNumber;

	/**
	 * Creates a Less-Than-Number ParameterConstraint, i.e. the value of the
	 * parameter to be tested has to be less than the given constraint value.
	 * 
	 * @param testNumber
	 *            parameter constraint value
	 */
	public LessConstraint(Number testNumber) {
		this.testNumber = testNumber;
	}

	/**
	 * Checks if the given number is less than the parameter constraint value.
	 * If not a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() >= testNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error: \n"
					+ "The parameter value specified has to be less than " + testNumber.toString()
					+ ".\n");
		}
	}

}
