package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Quadratic weight function, scaled using the standard deviation.
 * 
 * We needed another scaling here, we chose the cutoff point to be 3*stddev.
 * If you need another value, you have to reimplement this class.
 * 
 * max(0.0, 1.0 - (distance/(3*stddev))^2 
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class QuadraticStddevWeight implements WeightFunction {
  /**
   * Scaling: at scaling * stddev the function will hit 0.0
   */
  private static final double scaling = 3;
  /**
   * Evaluate weight function at given parameters. max is ignored.
   */
  public double getWeight(double distance, @SuppressWarnings("unused") double max, double stddev) {
    if (stddev <= 0) return 1;
    double scaleddistance = distance / (scaling * stddev);
    // After this, the result would be negative.
    if (scaleddistance >= 1.0) return 0.0;
    return 1.0 - scaleddistance * scaleddistance;
  }
}
