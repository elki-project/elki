package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Represents a Greater-Equal-Than-Number ParameterConstraint. The (number)
 * value of the parameter tested has to be greater equal than the specified
 * constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class GreaterEqualConstraint implements ParameterConstraint<Number> {

	/**
	 * parameter constraint value
	 */
	private Number testNumber;

	/**
	 * Creates a Greater-Equal- Parameter Constraint, i.e. the value of the
	 * parameter has to be greater equal than the given constraint value.
	 * 
	 * @param testNumber
	 *            constraint parameter value
	 */
	public GreaterEqualConstraint(Number testNumber) {
		this.testNumber = testNumber;
	}

	/**
	 * Checks if the given number is greater equal than the parameter constraint
	 * value. If not a paramter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() < testNumber.doubleValue()) {
			throw new WrongParameterValueException("Parameter Constraint Error: \n"
					+ "The parameter value specified has to be greater equal than "
					+ testNumber.toString() + ". (current value: "+t.doubleValue()+")\n");
		}
	}

}
