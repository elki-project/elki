package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Inverse proportional weight function, scaled using the standard deviation.
 * 
 * 1 / (1 + distance/stddev)
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class InverseProportionalStddevWeight implements WeightFunction {
  /**
   * Get inverse proportional weight. max is ignored.
   */
  public double getWeight(double distance, double max, double stddev) {
    if (stddev <= 0) return 1;
    double scaleddistance = distance / stddev;
    return 1 / (1 + scaleddistance);
  }
}