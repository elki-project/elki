package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

/**
 * Abstract superclass for specifying program arguments.
 *
 * @author Steffi Wanka
 * @param <T> the type of a possible value for this option
 */
public abstract class Option<T> extends AbstractLoggable {

  /**
   * The option name.
   */
  protected String name;

  /**
   * The option description.
   */
  protected String description;

  /**
   * The value of this option.
   */
  protected T value;

  /**
   * Sets the name and description of the option.
   *
   * @param name        The name of the option.
   * @param description The description of the option.
   */
  public Option(String name, String description) {
    super(LoggingConfiguration.DEBUG);
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
   * @param value the option's value to be set
   * @throws ParameterException if the given value is not a valid value for this option.
   */
  public abstract void setValue(String value) throws ParameterException;

  /**
   * Returns the value of the option.
   *
   * @return the option's value.
   * @throws UnusedParameterException is not thrown actually in this class
   *                                  but subclasses may require allowance to throw this Exception
   */
  public T getValue() throws UnusedParameterException {
    return this.value;
  }

  /**
   * Checks if the given argument is valid for this option.
   *
   * @param value option value to be checked
   * @return true, if the given value is valid for this option
   * @throws ParameterException if the given value is not a valid value for this option.
   */
  public abstract boolean isValid(String value) throws ParameterException;
}
