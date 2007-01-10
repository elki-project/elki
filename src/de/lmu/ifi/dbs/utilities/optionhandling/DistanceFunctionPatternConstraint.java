package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;

/**
 * Parameter Constraint class for testing if a given pattern parameter holds a valid pattern
 * for a specific distance function. 
 * 
 * @author Steffi Wanka
 *
 */
public class DistanceFunctionPatternConstraint implements ParameterConstraint<String> {

	/**
	 * The distance function the pattern is checked for. 
	 */	
	private DistanceFunction distFunction;
	
	/**
	 * Constructs a distance function pattern constraint for testing if a given pattern 
	 * parameter holds a valid pattern for the parameter distFunction
	 * 
	 * @param distFunction the distance function the pattern is checked for
	 */
	public DistanceFunctionPatternConstraint(DistanceFunction distFunction){
		this.distFunction = distFunction;
	}
	
	/**
	 * Checks if the given pattern parameter holds a valid pattern for the distance function. 
	 * If not so a ParameterException is thrown.
	 */
	public void test(String t) throws ParameterException {
		
		try{
		distFunction.valueOf(t);
		}catch(IllegalArgumentException ex){
			throw new WrongParameterValueException("The specified pattern "+t+" is not valid " +
					"for distance function "+distFunction.getClass().getName()+"!");
		}

	}

}
