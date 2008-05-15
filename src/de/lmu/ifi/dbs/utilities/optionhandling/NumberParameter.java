package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Abstract class for defining a number parameter.
 * @author Steffi Wanka
 *
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class NumberParameter<T extends Number> extends Parameter<T,Number> {

	/**
	 * Constructs a number parameter with the given name and description.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public NumberParameter(String name,String description){
		super(name,description);
	}
	
	/**
	 * Returns the number value of the parameter.
	 * 
	 * @return number value of the parameter.
	 */
	public T getNumberValue(){
		return value;
	}

}
