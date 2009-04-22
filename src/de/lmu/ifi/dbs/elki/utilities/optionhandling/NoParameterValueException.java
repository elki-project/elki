package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Thrown by OptionHandler in case of incorrect parameter-array.
 */
public class NoParameterValueException extends ParameterException {
  /**
   * Serial version UID
   */
  private static final long serialVersionUID = 8991076624591950629L;

  /**
   * Thrown by OptionHandler in case of incorrect parameter-array.
   *
   * @param message the detail message
   */
  public NoParameterValueException(String message) {
    super(message);
  }

  /**
   * Thrown by OptionHandler in case of incorrect parameter-array.
   *
   * @param message the detail message
   * @param cause   the cause
   */
  public NoParameterValueException(String message, Throwable cause) {
    super(message, cause);
  }
}
