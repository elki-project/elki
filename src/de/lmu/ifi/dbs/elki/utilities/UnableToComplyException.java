package de.lmu.ifi.dbs.elki.utilities;

/**
 * General Exception to state inability to execute an operation.
 * 
 * @author Arthur Zimek
 */
public class UnableToComplyException extends Exception {
  private static final long serialVersionUID = -5382132162693018191L;

  /**
   * Exception to state inability to execute an operation.
   * 
   */
  public UnableToComplyException() {
    super();
  }

  /**
   * Exception to state inability to execute an operation.
   * 
   * @param message a message to describe cause of exception
   */
  public UnableToComplyException(String message) {
    super(message);
  }

  /**
   * Exception to state inability to execute an operation.
   * 
   * @param message a message to describe cause of exception
   * @param cause cause of exception
   */
  public UnableToComplyException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception to state inability to execute an operation.
   * 
   * @param cause cause of exception
   */
  public UnableToComplyException(Throwable cause) {
    super(cause);
  }

}
