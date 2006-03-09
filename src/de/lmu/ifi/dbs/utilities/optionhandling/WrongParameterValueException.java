package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Thrown by a Parameterizable object in case of wrong parameter format.
 */
@SuppressWarnings("serial")
public class WrongParameterValueException extends RuntimeException {
  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   *
   * @param parameter the parameter that has a wrong value
   * @param read      the value of the parameter read by the option handler
   */
  public WrongParameterValueException(String parameter, String read) {
    super("Wrong value of parameter " + parameter + ", read " + read + ".");
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   *
   * @param parameter the parameter that has a wrong value
   * @param read      the value of the parameter read by the option handler
   * @param expected additional message
   */
  public WrongParameterValueException(String parameter, String read, String expected) {
    super("Wrong value of parameter " + parameter + ", read " + read + ", expected: " + expected + ".");
  }
}
