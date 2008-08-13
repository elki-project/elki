package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class InverseProportionalStddevWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    assert (stddev > 0);
    double scaleddistance = distance / stddev;
    return 1 / (1 + scaleddistance);
  }
}