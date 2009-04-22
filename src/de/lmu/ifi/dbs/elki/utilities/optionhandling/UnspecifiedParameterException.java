package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Exception when a required parameter was not given.
 * @author Erich Schubert
 */
public class UnspecifiedParameterException extends WrongParameterValueException {
  /**
   * Serial UID
   */
  private static final long serialVersionUID = -7142809547201980898L;

  /**
   * Constructor with missing Parameter
   * @param parameter Missing parameter
   */
  public UnspecifiedParameterException(Parameter<?, ?> parameter) {
    super("No value given for parameter " + parameter.getName() + "\n" + "Expected: " + parameter.getDescription());
  }

  /**
   * Constructor with missing Parameter and cause
   * @param parameter Missing parameter
   * @param cause Cause
   */
  public UnspecifiedParameterException(Parameter<?, ?> parameter, Throwable cause) {
    super("No value given for parameter " + parameter.getName() + "\n" + "Expected: " + parameter.getDescription(), cause);
  }

  /**
   * Constructor with error message.
   * @param message
   */
  public UnspecifiedParameterException(String message) {
    super(message);
  }
}
