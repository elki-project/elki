package de.lmu.ifi.dbs.elki.utilities.exceptions;

/**
 * This class indicates an exception likely caused by an API not implemented
 * correctly. ELKI has some API interface restrictions that cannot be expressed
 * in the Java language and that are checked at runtime. This in particular
 * includes parameterization and dynamic binding of algorithms to data.
 * 
 * This exception indicates that such an API might not have been implemented
 * correctly; you should check the documentation of interfaces you implement for
 * API descriptions that are not expressible in Java (and thus not checked by
 * the Java compiler).
 * 
 * @author Erich Schubert
 */
public class APIViolationException extends AbortException {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   * 
   * @param message Error message
   */
  public APIViolationException(String message) {
    super(message);
  }

  /**
   * Constructor.
   * 
   * @param message Error message
   * @param cause Reason
   */
  public APIViolationException(String message, Throwable cause) {
    super(message, cause);
  }
}
