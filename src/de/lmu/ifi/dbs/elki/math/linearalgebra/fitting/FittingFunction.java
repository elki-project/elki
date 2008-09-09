package de.lmu.ifi.dbs.elki.math.linearalgebra.fitting;


/**
 * Interface for a function used in Levenberg-Marquard-Fitting
 * 
 * @author Erich Schubert
 */
public interface FittingFunction {
  /**
   * Compute value at position x as well as gradients for the parameters
   * 
   * @param x Current coordinate
   * @param params Function parameters parameters
   * @return Array consisting of y value and parameter gradients
   */
  public FittingFunctionResult eval(double x, double[] params);
}
