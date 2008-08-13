package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

import de.lmu.ifi.dbs.elki.math.ErrorFunctions;

public class ErfcWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    // FIXME: this scaling 2x is picked manually
    return ErrorFunctions.erfc(2 * relativedistance) * .9 + .1;
  }
}
