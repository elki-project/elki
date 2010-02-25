package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

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
   */
  public WrongParameterValueException(Parameter<?,?> parameter, String read) {
    this("Wrong value of parameter \"" + parameter.getName() + "\".\n" + "Read: " + read + ".\n" + "Expected: " + parameter.getFullDescription());
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read by the option handler
   * @param cause the cause
   */
  public WrongParameterValueException(Parameter<?, ?> parameter, String read, Throwable cause) {
    this("Wrong value of parameter \"" + parameter.getName() + "\".\n" + "Read: " + read + ".\n" + "Expected: " + parameter.getFullDescription(), cause);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read by the option handler
   * @param reason detailed error description
   * @param cause the cause
   */
  public WrongParameterValueException(Parameter<?, ?> parameter, String read, String reason, Throwable cause) {
    this("Wrong value of parameter " + parameter.getName() + ".\n" + "Read: " + read + ".\n" + "Expected: " + parameter.getFullDescription()+"\n"+reason, cause);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read by the option handler
   * @param reason detailed error description
   */
  public WrongParameterValueException(Parameter<?, ?> parameter, String read, String reason) {
    this("Wrong value of parameter " + parameter.getName() + ".\n" + "Read: " + read + ".\n" + "Expected: " + parameter.getFullDescription()+"\n"+reason);
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