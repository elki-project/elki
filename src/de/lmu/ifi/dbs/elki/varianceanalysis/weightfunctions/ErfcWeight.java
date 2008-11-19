package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

import de.lmu.ifi.dbs.elki.math.ErrorFunctions;

/**
 * Gaussian Error Function Weight function, scaled such that the result it 0.1 at distance == max
 * 
 * erfc(1.1630871536766736 * distance / max)
 * 
 * The value of 1.1630871536766736 is erfcinv(0.1), to achieve the intended scaling.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class ErfcWeight implements WeightFunction {
  /**
   * Get Erfc Weight, using distance / max. stddev is ignored.
   */
  public double getWeight(double distance, double max, @SuppressWarnings("unused") double stddev) {
    if (max <= 0) return 1.0;
    double relativedistance = distance / max;
    // the scaling was picked such that getWeight(a,a,0) is 0.1
    // since erfc(1.1630871536766736) == 1.0
    return ErrorFunctions.erfc(1.1630871536766736 * relativedistance);
  }
}
