package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

import de.lmu.ifi.dbs.elki.math.ErrorFunctions;

public class ErfcStddevWeight implements WeightFunction {
  private double onebysqrt2 = 1 / Math.sqrt(2);

  public double getWeight(double distance, double max, double stddev) {
    assert (stddev > 0);
    return ErrorFunctions.erfc(onebysqrt2 * distance / stddev);
  }
}
