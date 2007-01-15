package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Equal-Than GlobalParameterConstraint. The value of the
 * first NumberParameter has to be less equal than the value of the second
 * NumberParameter.
 * 
 * @author Steffi Wanka
 * 
 */
public class LessEqualGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * first NumberParameter
	 */
	private NumberParameter first;

	/**
	 * second NumberParameter
	 */
	private NumberParameter second;

	/**
	 * Creates a Less-Equal-Than GlobalParameterConstraint, i.e. the value of
	 * the first NumberParameter given has to be less equal than the value of
	 * the second NumberParameter given.
	 * 
	 * @param first
	 *            first NumberParameter
	 * @param sec
	 *            second NumberParameter
	 */
	public LessEqualGlobalConstraint(NumberParameter first, NumberParameter sec) {
		this.first = first;
		this.second = sec;
	}

	/**
	 * Checks if the value of the first NumberParameter is less equal than the
	 * value of the second NumberParameter. If not a Parameter Exception is
	 * thrown.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
	 */
	public void test() throws ParameterException {

		if (first.isSet() && second.isSet()) {
			if (first.getNumberValue().doubleValue() > second.getNumberValue().doubleValue()) {

				throw new WrongParameterValueException("Global Parameter Constraint Error: \n" + "The value of parameter \""
						+ first.getName() + "\" has to be less equal than the" + "value of parameter \"" + second.getName() + "\"!\n");
			}
		}
	}

}
