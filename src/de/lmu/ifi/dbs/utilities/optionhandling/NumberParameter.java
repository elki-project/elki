package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Abstract class for defining a number parameter.
 * @author Steffi Wanka
 *
 * @param <T>
 */
public abstract class NumberParameter<T extends Number> extends Parameter<Number>{

	public NumberParameter(String name,String description){
		super(name,description);
	}
	
	/**
	 * Returns the number value of the parameter.
	 * 
	 * @return number value of the parameter.
	 */
	public abstract Number getNumberValue();

}
