package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a List-Size GlobalParameterConstraint. The size of the
 * ListParameter tested has to be equal to the constraint size given by the
 * integer parameter.
 * 
 * @author Steffi Wanka
 * 
 */
public class GlobalListSizeConstraint implements GlobalParameterConstraint {

	/**
	 * ListParameter to be tested
	 */
	private ListParameter list;

	/**
	 * constraint list size
	 */
	private IntParameter length;

	/**
	 * Creates a List-Size GlobalParameterConstraint, i.e. the size of the given
	 * ListParameter hat to be equal to the constraint list size given by the
	 * integer parameter.
	 * 
	 * @param v
	 *            the ListParameter to be tested.
	 * @param i
	 *            constraint list size.
	 */
	public GlobalListSizeConstraint(ListParameter v, IntParameter i) {
		this.list = v;
		this.length = i;
	}

	/**
	 * Checks is the size of the ListParameter is equal to the constraint list
	 * size specified. If not a parameter exception is thrown.
	 * 
	 */
	public void test() throws ParameterException {

		if (list.getListSize() != length.getValue()) {
			throw new WrongParameterValueException("Global Parameter Constraint Error!" +
                                             "\nThe size of the list parameter \"" +
                                             list.getName() +
                                             "\" must be " +
                                             length.getValue() +
                                             ", but is " + list.getListSize() +
                                             ". The value is given by the integer parameter " + 
                                             length.getName() + "!\n");
		}
	}

}
