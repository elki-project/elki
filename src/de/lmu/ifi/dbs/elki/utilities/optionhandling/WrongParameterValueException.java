package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Thrown by a Parameterizable object in case of wrong parameter format.
 */
public class WrongParameterValueException extends ParameterException {

  /**
   * Generated serial version UID.
   */
  private static final long serialVersionUID = 2155964376772417402L;

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read by the option handler
   * @param expected the value that has been expected
   */
  public WrongParameterValueException(String parameter, String read, String expected) {
    super("Wrong value of parameter " + parameter + ".\nRead " + read + ".\nExpected: " + expected);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read by the option handler
   * @param expected the value that has been expected
   * @param cause the cause
   */
  public WrongParameterValueException(String parameter, String read, String expected, Throwable cause) {
    super("Wrong value of parameter " + parameter + ".\n" + "Read: " + read + ".\n" + "Expected: " + expected, cause);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read by the option handler
   * @param cause the cause
   */
  public WrongParameterValueException(Parameter<?, ?> parameter, String read, Throwable cause) {
    super("Wrong value of parameter " + parameter.getName() + ".\n" + "Read: " + read + ".\n" + "Expected: " + parameter.getDescription(), cause);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param message detail message
   */
  public WrongParameterValueException(String message) {
    super(message);
  }
  
  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param message detail message
   * @param e cause
   */
  public WrongParameterValueException(String message, Throwable e) {
    super(message,e );
  }
}
