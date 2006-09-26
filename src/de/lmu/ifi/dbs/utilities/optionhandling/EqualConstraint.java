package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Represents an Equal-To-Number ParameterConstraint. 
 * The (number) value of the parameter tested has to be equal to the specified constraint value.
 * 
 * @author Steffi Wanka
 *
 */
public class EqualConstraint implements ParameterConstraint<Number> {

	/**
	 * parameter constraint value
	 */
	private Number testNumber;

	/**
	 * Creates an Equal-To-Number Parameter Constraint, i.e. the value of the parameter
	 * has to be equal to the given constraint value
	 * 
	 * @param testNumber parameter constraint value
	 */
	public EqualConstraint(Number testNumber) {
		this.testNumber = testNumber;
	}

	/**
	 * Checks if the given number is equal to the respective parameter constraint value. If not
	 * a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {

		if (t.doubleValue() != testNumber.doubleValue()) {
			throw new WrongParameterValueException("");
		}
	}

}
