package de.lmu.ifi.dbs.math.linearalgebra;

import de.lmu.ifi.dbs.utilities.Util;

/**
 * Helper class which encapsulates an eigenvector and its corresponding
 * eigenvalue. This class is used to sort eigenpairs.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class EigenPair implements Comparable<EigenPair> {
  /**
   * The eigenvector as a matrix.
   */
  private Matrix eigenvector;

  /**
   * The corresponding eigenvalue.
   */
  private double eigenvalue;

  /**
   * Creates a new EigenPair object.
   *
   * @param eigenvector the eigenvector as a matrix
   * @param eigenvalue  the corresponding eigenvalue
   */
  public EigenPair(Matrix eigenvector, double eigenvalue) {
    this.eigenvalue = eigenvalue;
    this.eigenvector = eigenvector;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object's
   * eigenvalue is greater than, equal to, or less than the specified
   * object's eigenvalue.
   *
   * @param o the Eigenvector to be compared.
   * @return a negative integer, zero, or a positive integer as this
   *         object's eigenvalue is greater than, equal to, or less than
   *         the specified object's eigenvalue.
   */
  public int compareTo(EigenPair o) {
    if (this.eigenvalue < o.eigenvalue)
      return -1;
    if (this.eigenvalue > o.eigenvalue)
      return +1;
    return 0;
  }

  /**
   * Returns a string representation of this EigenPair.
   *
   * @return a string representation of this EigenPair
   */
  public String toString() {
    return "(ew = " + Util.format(eigenvalue) + ", ev = ["
           + Util.format(eigenvector.getColumnPackedCopy()) + "])";
  }
}
