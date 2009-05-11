package de.lmu.ifi.dbs.elki.algorithm;

/**
 * Exception for aborting some process and transporting a message.
 * 
 * @author Arthur Zimek
 */
public class AbortException extends RuntimeException {
  /**
   * Serial UID.
   */
  private static final long serialVersionUID = -1128409354869276998L;

  /**
   * Exception for aborting some process and transporting a message.
   * 
   * @param message message to be transported
   */
  public AbortException(String message) {
    super(message);
  }

  /**
   * Exception for aborting some process and transporting a message.
   * 
   * @param message message to be transported
   * @param cause cause of this exception
   */
  public AbortException(String message, Throwable cause) {
    super(message, cause);
  }
}
