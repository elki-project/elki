package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Constant Weight function
 * 
 * The result is always 1.0
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class ConstantWeight implements WeightFunction {
  /**
   * Get the constant weight
   * No scaling - the result is always 1.0
   */
  public double getWeight(double distance, double max, double stddev) {
    return 1.0;
  }
}
