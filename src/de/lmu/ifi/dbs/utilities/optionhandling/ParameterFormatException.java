package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Thrown by a Parameterizable object in case of wrong parameter format.
 */
@SuppressWarnings("serial")
public class ParameterFormatException extends RuntimeException {
  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   */
  public ParameterFormatException(String parameter, String read) {
    super("Wrong format of parameter " + parameter + ", read " + read);
  }
}
