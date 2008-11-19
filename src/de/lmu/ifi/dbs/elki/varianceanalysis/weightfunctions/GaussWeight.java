package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Gaussian Weight function, scaled such that the result it 0.1 at distance == max
 * 
 * exp(-2.3025850929940455 * (distance/max)^2)
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class GaussWeight implements WeightFunction {
  /**
   * Get Gaussian weight. stddev is not used, scaled using max.
   */
  public double getWeight(double distance, double max, @SuppressWarnings("unused") double stddev) {
    if (max <= 0) return 1.0;
    double relativedistance = distance / max;
    // -2.303 is log(.1) to suit the intended range of 1.0-0.1
    return java.lang.Math.exp(-2.3025850929940455 * relativedistance * relativedistance);
  }
}
