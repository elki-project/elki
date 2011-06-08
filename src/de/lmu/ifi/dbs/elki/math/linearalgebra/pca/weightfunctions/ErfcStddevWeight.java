package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Gaussian Error Function Weight function, scaled using stddev. This probably
 * is the most statistically sound weight.
 * 
 * erfc(1 / sqrt(2) * distance / stddev)
 * 
 * @author Erich Schubert
 */
public final class ErfcStddevWeight implements WeightFunction {
  /**
   * Return Erfc weight, scaled by standard deviation. max is ignored.
   */
  @Override
  public double getWeight(double distance, @SuppressWarnings("unused") double max, double stddev) {
    if(stddev <= 0) {
      return 1;
    }
    return ErrorFunctions.erfc(MathUtil.SQRTHALF * distance / stddev);
  }
}
