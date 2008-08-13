package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class LinearWeight implements WeightFunction {
  /**
   * Linear decreasing weight, from 1.0 to 0.1
   */
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    return 1 - relativedistance * .9;
  }

}
