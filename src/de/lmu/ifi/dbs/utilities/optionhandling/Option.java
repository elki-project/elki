package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Abstract class for holding program arguments.
 * 
 * @author Steffi Wanka
 *
 */
public abstract class Option {

	
	/**
	 * The name of the option.,
	 */
	protected String name;
	
	/**
	 * The description of the option.
	 */
	protected String description;
	
	/**
	 * Sets the name and description of the option.
	 * 
	 * @param name The name of the option.
	 * @param description The description of the option.
	 */
	public Option(String name, String description){
		this.name = name;
		this.description = description;
	}
	
	/**
	 * Returns the name of the option.
	 * 
	 * @return the option's name.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Returns the description of the option.
	 * 
	 * @return the option's description.
	 */
	public String getDescription(){
		return description;
	}
	
	/**
	 * Returns true if the value of the option is set, false otherwise.
	 * 
	 * @return true if the value of the option is set, false otherwise.
	 */
	public abstract boolean isSet();
	
	/**
	 * Sets the value of the option.
	 * 
	 * @param value the option's value to be set
	 */
	public abstract void setValue( String value);
	
	/**
	 * Returns the value of the option.
	 * 
	 * @return the option's value.
	 */
	public abstract String getValue();
}
