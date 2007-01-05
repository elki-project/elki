package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Represents an Equal-To-String ParameterConstraint. The string value of the
 * parameter tested has to be equal to the specified string constraint value.
 * 
 * @author Steffi Wanka
 * 
 */
public class EqualStringConstraint implements ParameterConstraint<String> {

	/**
	 * parameter constraint String
	 */
	private String[] testStrings;

	/**
	 * Creates an Equal-To-String Parameter Constraint, i.e. the string value of
	 * the parameter has to be equal to one of the given constraint string
	 * values.
	 * 
	 * @param testString
	 *            parameter constraint string.
	 */
	public EqualStringConstraint(String[] testStrings) {
		this.testStrings = testStrings;
	}

	private String constraintStrings() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		for (int i = 0; i < testStrings.length; i++) {
			buffer.append(testStrings[i]);
			if (i != testStrings.length - 1) {
				buffer.append(",");
			}
		}

		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Checks if the given string value is equal to the constraint string value.
	 * If not, a parameter exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(String t) throws ParameterException {

		for (String constraint : testStrings) {
			if (t.equalsIgnoreCase(constraint)) {
				return;
			}
		}

		throw new WrongParameterValueException("Parameter Constraint Error!\n" + "Parameter value must be one of the following values: "
				+ constraintStrings());

	}

}
