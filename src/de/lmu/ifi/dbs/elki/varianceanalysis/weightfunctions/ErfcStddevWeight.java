package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

import de.lmu.ifi.dbs.elki.math.ErrorFunctions;

/**
 * Gaussian Error Function Weight function, scaled using stddev.
 * This probably is the most statistically sound weight.
 * 
 * erfc(1 / sqrt(2) * distance / stddev)
 * 
 * {@see http://en.wikipedia.org/wiki/Error_function}
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class ErfcStddevWeight implements WeightFunction {
  /**
   * Precomputed value 1 / sqrt(2)
   */
  private static final double onebysqrt2 = 1 / Math.sqrt(2);

  /**
   * Return Erfc weight, scaled by standard deviation. max is ignored.
   */
  public double getWeight(double distance, double max, double stddev) {
    if (stddev <= 0) return 1;
    return ErrorFunctions.erfc(onebysqrt2 * distance / stddev);
  }
}
