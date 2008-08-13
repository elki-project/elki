package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class InverseProportionalWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    return 1 / (1 + 9 * relativedistance);
  }
}