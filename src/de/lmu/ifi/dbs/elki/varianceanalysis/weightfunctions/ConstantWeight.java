package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class ConstantWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    return 1.0;
  }
}
