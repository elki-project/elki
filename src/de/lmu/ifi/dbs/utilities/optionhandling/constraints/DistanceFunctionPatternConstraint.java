package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter constraint class for testing if a given pattern parameter ({@link de.lmu.ifi.dbs.utilities.optionhandling.PatternParameter}) 
 * holds a valid pattern for a specific distance function ({@link de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction}).
 * 
 * @author Steffi Wanka
 * 
 */
public class DistanceFunctionPatternConstraint implements ParameterConstraint<String> {

	/**
	 * The distance function the pattern is checked for.
	 */
	private DistanceFunction<?,?> distFunction;

	/**
	 * Constructs a distance function pattern constraint for testing if a given
	 * pattern parameter holds a valid pattern for the parameter {@code distFunction}
	 * 
	 * @param distFunction
	 *            the distance function the pattern is checked for
	 */
	public DistanceFunctionPatternConstraint(DistanceFunction<?,?> distFunction) {
		this.distFunction = distFunction;
	}

	/**
	 * Checks if the given pattern parameter holds a valid pattern for the
	 * distance function. If not so, a parameter exception ({@link de.lmu.ifi.dbs.utilities.optionhandling.ParameterException}) is thrown.
	 * 
	 * @see ParameterConstraint#test(Object)
	 */
	public void test(String t) throws ParameterException {

		try {
			distFunction.valueOf(t);
		} catch (IllegalArgumentException ex) {
			throw new WrongParameterValueException("The specified pattern " + t + " is not valid " + "for distance function "
					+ distFunction.getClass().getName() + "!");
		}

	}

}
