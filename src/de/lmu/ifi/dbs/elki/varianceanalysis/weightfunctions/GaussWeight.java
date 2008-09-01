package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class GaussWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    // FIXME: -2.303 is log(-.1) to suit the intended range of 1.0-0.1
    return java.lang.Math.exp(-2.3025850929940455 * relativedistance * relativedistance);
  }
}
