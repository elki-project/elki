package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Exponential Weight function, scaled such that the result it 0.1 at distance
 * == max
 * 
 * stddev * exp(-.5 * distance/stddev)
 * 
 * This is similar to the Gaussian weight function, except distance/stddev is
 * not squared.
 * 
 * @author Erich Schubert
 */
public final class ExponentialStddevWeight implements WeightFunction {
  /**
   * Get exponential weight, max is ignored.
   */
  public double getWeight(double distance, @SuppressWarnings("unused") double max, double stddev) {
    if(stddev <= 0) {
      return 1;
    }
    double scaleddistance = distance / stddev;
    return stddev * java.lang.Math.exp(-.5 * scaleddistance);
  }
}