package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Exponential Weight function, scaled such that the result it 0.1 at distance == max
 * 
 * exp(-2.3025850929940455 * distance/max)
 * 
 * This is similar to the Gaussian weight function, except distance/max is not squared.
 * 
 * -2.3025850929940455 is log(-.1) to achieve the intended range of 1.0 - 0.1
 * 
 * @author Erich Schubert
 */
public final class ExponentialWeight implements WeightFunction {
  /**
   * Exponential Weight function. stddev is not used.
   */
  public double getWeight(double distance, double max, @SuppressWarnings("unused") double stddev) {
    if (max <= 0) return 1.0;
    double relativedistance = distance / max;
    // scaling -2.303 is log(-.1) to suit the intended range of 1.0-0.1
    return java.lang.Math.exp(-2.3025850929940455 * relativedistance);
  }
}
