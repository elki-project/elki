package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Gaussian Weight function, scaled such using standard deviation
 * 
 * factor * exp(-.5 * (distance/stddev)^2)
 * 
 * with factor being 1 / sqrt(2 * PI)
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class GaussStddevWeight implements WeightFunction {
  /**
   * Constant scaling factor of Gaussian distribution.
   * 
   * In fact, in most use cases we could leave this away.
   */
  private final static double scaling = 1 / Math.sqrt(2 * Math.PI);

  /**
   * Get Gaussian Weight using standard deviation for scaling.
   * max is ignored. 
   */
  public double getWeight(double distance, @SuppressWarnings("unused") double max, double stddev) {
    if (stddev <= 0) return 1;
    double normdistance = distance / stddev;
    return scaling * java.lang.Math.exp(-.5 * normdistance * normdistance) / stddev;
  }
}
