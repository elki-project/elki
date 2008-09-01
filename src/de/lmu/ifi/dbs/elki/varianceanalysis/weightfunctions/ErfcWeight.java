package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

import de.lmu.ifi.dbs.elki.math.ErrorFunctions;

public class ErfcWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    // the scaling was picked such that getWeight(a,a,0) is 0.1
    // since erfc(1.1630871536766736) == 1.0
    return ErrorFunctions.erfc(1.1630871536766736 * relativedistance);
  }
}
