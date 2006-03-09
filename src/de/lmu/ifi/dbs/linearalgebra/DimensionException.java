package de.lmu.ifi.dbs.linearalgebra;

/**
 * Class for dimension exceptions.
 */
public class DimensionException extends Exception {

  /**
   * Constructs a <code>DimensionException</code>
   * with no detail message.
   */
  public DimensionException() {
    super();
  }

  /**
   * Constructs a <code>DimensionException</code> with the
   * specified detail message.
   *
   * @param s the detail message.
   */
  public DimensionException(String s) {
    super(s);
  }
}
