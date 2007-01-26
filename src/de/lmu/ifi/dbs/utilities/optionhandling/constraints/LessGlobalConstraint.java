package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Than GlobalParameterConstraint. The value of the first
 * NumberParameter has to be less than the value of the second NumberParameter
 * specified.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * first NumberParameter //
	 */
	private NumberParameter first;

	/**
	 * second NumberParameter
	 */
	private NumberParameter second;

	/**
	 * Creates a Less-Than GlobalParameterConstraint, i.e. the value of the
	 * first NumberParameter given has to be less than the value of the second
	 * NumberParameter given.
	 * 
	 * @param first
	 *            first NumberParameter
	 * @param second
	 *            second NumberParameter
	 */
	public LessGlobalConstraint(NumberParameter first, NumberParameter second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Checks if the value of the first NumberParameter is less than the value
	 * of the second NumberParameter. If not a Parameter Exception is thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		if (first.getNumberValue().doubleValue() >= second.getNumberValue().doubleValue()) {

			throw new WrongParameterValueException("Global Parameter Constraint Error: \n" + "The value of parameter \""
					+ first.getName() + "\" has to be less than the" + "value of parameter \""
					+ second.getName() + "\"!\n");
		}

	}

}
