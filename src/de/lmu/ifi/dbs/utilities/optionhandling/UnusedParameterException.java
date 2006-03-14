package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Thrown by OptionHandler in case of request of an unused parameter.
 */
@SuppressWarnings("serial")
public class UnusedParameterException extends ParameterException {

  /**
   * Thrown by OptionHandler in case of request of an unused parameter.
   *
   * @param message the detail message
   */
  public UnusedParameterException(String message) {
    super(message);
  }
}
