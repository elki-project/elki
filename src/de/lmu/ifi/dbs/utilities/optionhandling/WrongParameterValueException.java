package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Thrown by a Parameterizable object in case of wrong parameter format.
 */
@SuppressWarnings("serial")
public class WrongParameterValueException extends ParameterException {
  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   *
   * @param parameter the parameter that has a wrong value
   * @param read      the value of the parameter read by the option handler
   * @param expected  the value that has been expected
   */
  public WrongParameterValueException(String parameter, String read, String expected) {
    super("Wrong value of parameter " + parameter +
          ".\nRead " + read +
          ".\nExpected: " + expected);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   *
   * @param parameter the parameter that has a wrong value
   * @param read      the value of the parameter read by the option handler
   * @param expected  the value that has been expected
   * @param cause     the cause
   */
  public WrongParameterValueException(String parameter, String read, String expected, Throwable cause) {
    super("Wrong value of parameter " + parameter +
          ".\nRead " + read +
          ".\nExpected: " + expected +
          ".\nCaused by " + cause);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   *
   * @param message detail message
   */
  public WrongParameterValueException(String message) {
    super(message);
  }
}
