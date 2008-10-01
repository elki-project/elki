package de.lmu.ifi.dbs.elki.math.linearalgebra;

import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * Helper class which encapsulates an eigenvector and its corresponding
 * eigenvalue. This class is used to sort eigenpairs.
 *
 * @author Elke Achtert 
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
   * Returns the eigenvector.
   * @return the eigenvector
   */
  public Matrix getEigenvector() {
    return eigenvector;
  }

  /**
   * Returns the eigenvalue.
   * @return the eigenvalue
   */
  public double getEigenvalue() {
    return eigenvalue;
  }

  /**
   * Returns a string representation of this EigenPair.
   *
   * @return a string representation of this EigenPair
   */
  @Override
  public String toString() {
    return "(ew = " + Util.format(eigenvalue) + ", ev = ["
           + Util.format(eigenvector.getColumnPackedCopy()) + "])";
  }
}
