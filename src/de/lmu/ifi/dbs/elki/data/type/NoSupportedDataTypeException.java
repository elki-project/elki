package de.lmu.ifi.dbs.elki.data.type;

/**
 * Exception thrown when no supported data type was found.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses TypeInformation oneway - -
 */
public class NoSupportedDataTypeException extends IllegalStateException {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public NoSupportedDataTypeException(TypeInformation type) {
    super("No data type found satisfying: " + type.toString());
  }

  /**
   * Constructor with string message. If possible, use the type parameter instead!
   *
   * @param string Error message
   */
  public NoSupportedDataTypeException(String string) {
    super(string);
  }
}