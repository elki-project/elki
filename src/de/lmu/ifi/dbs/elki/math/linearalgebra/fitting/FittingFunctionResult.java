package de.lmu.ifi.dbs.elki.math.linearalgebra.fitting;

/**
 * Result returned by a fitting function.
 * 
 * @author Erich Schubert
 */
public class FittingFunctionResult {
  /**
   * Value at the given coordinate
   */
  public double y;
  /**
   * Parameter gradients at the given coordinate
   */
  public double[] gradients;
  
  /**
   * Trivial/generic constructor for the result class
   * 
   * @param y value at the coordinate
   * @param gradients parameter gradients
   */
  public FittingFunctionResult(double y, double[] gradients) {
    super();
    this.y = y;
    this.gradients = gradients;
  }
}
