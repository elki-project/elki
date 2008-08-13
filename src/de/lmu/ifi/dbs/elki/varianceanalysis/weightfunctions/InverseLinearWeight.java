package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class InverseLinearWeight implements WeightFunction {
  /**
   * Linear increasing weight, from 0.1 to 1.0
   * 
   * NOTE: increasing weights are non-standard, and mostly for proof-of-concept
   * Although there might be a clever algorithm some day that benefits from it.
   */
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    return .1 + relativedistance * .9;
  }
}