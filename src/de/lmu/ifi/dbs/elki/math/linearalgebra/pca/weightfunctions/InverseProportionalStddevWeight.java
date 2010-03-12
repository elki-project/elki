package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Inverse proportional weight function, scaled using the standard deviation.
 * 
 * 1 / (1 + distance/stddev)
 * 
 * @author Erich Schubert
 */
public final class InverseProportionalStddevWeight implements WeightFunction {
  /**
   * Get inverse proportional weight. max is ignored.
   */
  public double getWeight(double distance, @SuppressWarnings("unused") double max, double stddev) {
    if(stddev <= 0) {
      return 1;
    }
    double scaleddistance = distance / stddev;
    return 1 / (1 + scaleddistance);
  }
}