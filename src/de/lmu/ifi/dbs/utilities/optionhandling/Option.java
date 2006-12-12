package de.lmu.ifi.dbs.utilities.optionhandling;


/**
 * Abstract class for holding program arguments.
 * 
 * @author Steffi Wanka
 * 
 */
public abstract class Option<T> {

	/**
	 * The name of the option.
	 */
	protected String name;

	/**
	 * The description of the option.
	 */
	protected String description;

  /**
   * The value of this option.
   */
  protected T value;

	/**
	 * Sets the name and description of the option.
	 * 
	 * @param name
	 *            The name of the option.
	 * @param description
	 *            The description of the option.
	 */
	public Option(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Returns the name of the option.
	 * 
	 * @return the option's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the description of the option.
	 * 
	 * @return the option's description.
	 */
	public String getDescription() {
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
	 * @param value
	 *            the option's value to be set
	 */
	public abstract void setValue(String value) throws ParameterException;

	/**
	 * Returns the value of the option.
	 * 
	 * @return the option's value.
	 */
	public abstract T getValue() throws UnusedParameterException;

  /**
   * todo comment
   * @param value
   * @return
   * @throws ParameterException
   */
  public abstract boolean isValid(String value) throws ParameterException;

}
