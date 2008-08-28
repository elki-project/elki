package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Abstract super class for all exceptions thrown during parameterization.
 * 
 * @author Elke Achtert
 */
public abstract class ParameterException extends Exception {
  private static final long serialVersionUID = 5900443690403902986L;

  protected ParameterException(String message) {
    super(message);
  }

  protected ParameterException(String message, Throwable cause) {
    super(message, cause);
  }
}
