package de.lmu.ifi.dbs.elki.normalization;

/**
 * An exception to signal the encounter of non numeric features where numeric
 * features have been expected.
 * 
 * @author Arthur Zimek
 */
public class NonNumericFeaturesException extends Exception {

  /**
   * Generated serial version UID.
   */
  private static final long serialVersionUID = 284302959521511627L;

  /**
   * An exception to signal the encounter of non numeric features where numeric
   * features have been expected.
   * 
   * @see Exception
   */
  public NonNumericFeaturesException() {
    super();
  }

  /**
   * An exception to signal the encounter of non numeric features where numeric
   * features have been expected.
   * 
   * @param message Message
   * @see Exception
   */
  public NonNumericFeaturesException(String message) {
    super(message);
  }

  /**
   * An exception to signal the encounter of non numeric features where numeric
   * features have been expected.
   * 
   * @param cause Throwable cause
   * @see Exception
   */
  public NonNumericFeaturesException(Throwable cause) {
    super(cause);
  }

  /**
   * An exception to signal the encounter of non numeric features where numeric
   * features have been expected.
   * 
   * @param message Message
   * @param cause Throwable Cause
   * @see Exception
   */
  public NonNumericFeaturesException(String message, Throwable cause) {
    super(message, cause);
  }
}