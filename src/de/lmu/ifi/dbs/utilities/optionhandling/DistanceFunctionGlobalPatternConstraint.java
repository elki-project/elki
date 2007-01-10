package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Global parameter constraint for testing if a given pattern parameter specifies a valid
 * pattern for given class parameter defining a specific distance function.
 * 
 * @author Steffi Wanka
 *
 */
public class DistanceFunctionGlobalPatternConstraint implements GlobalParameterConstraint {

	/**
	 * Class parameter whose restriction class is used to check the validity of the pattern parameter
	 */
	private ClassParameter restrictionClass;
	
	/**
	 * Pattern parameter to be checked for validity.
	 */
	private PatternParameter pattern;
	
	/**
	 * Constructs a global parameter constraint for testing if a given pattern parameter is a valid
	 * argument for a given distance function class parameter.
	 * 
	 * @param pattern the pattern parameter
	 * @param restrClass the class parameter defining a distance function
	 */
	public DistanceFunctionGlobalPatternConstraint(PatternParameter pattern, ClassParameter restrClass){
		this.pattern = pattern;
		restrictionClass = restrClass;
	}
	
	/**
	 * Tests if the pattern is valid for the distance function. If not so a ParameterValueException
	 * is thrown.
	 */
	public void test() throws ParameterException {
		
		
		if(restrictionClass.getRestrictionClass() == null){
			throw new WrongParameterValueException("Global parameter constraint error!\n" +
					"Restriction class of class parameter "+restrictionClass.getName()+" is null!");
		}

		if(!DistanceFunction.class.isAssignableFrom(restrictionClass.getRestrictionClass())){
			throw new WrongParameterValueException("Global parameter constraint error!\n" +
					"Class parameter "+restrictionClass.getName()+ "doesn't specify a distance function!");
		}
		Class<DistanceFunction> restrClass = restrictionClass.getRestrictionClass();
		
		
		try {
			DistanceFunction func = Util.instantiate(restrClass, restrictionClass.getValue());
			func.valueOf(pattern.getValue());
			
		} catch(IllegalArgumentException e){
			throw new WrongParameterValueException("Global parameter constraint error!\n" +
					"Pattern parameter "+pattern.getName()+" is no valid pattern for " +
							"distance function "+restrictionClass.getName()+"!");
		}
		catch (UnableToComplyException e) {
			throw new WrongParameterValueException("Global Parameter Constraint Error!\n" +
					"Cannot instantiate distance function!");
		}
		
	}

}
