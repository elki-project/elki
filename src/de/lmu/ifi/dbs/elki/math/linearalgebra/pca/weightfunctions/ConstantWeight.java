package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Constant Weight function
 * 
 * The result is always 1.0
 * 
 * @author Erich Schubert
 */
public final class ConstantWeight implements WeightFunction {
  /**
   * Get the constant weight
   * No scaling - the result is always 1.0
   */
  public double getWeight(@SuppressWarnings("unused") double distance, @SuppressWarnings("unused") double max, @SuppressWarnings("unused") double stddev) {
    return 1.0;
  }
}
