package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class ExponentialStddevWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    assert (stddev > 0);
    double scaleddistance = distance / stddev;
    return stddev * java.lang.Math.exp(-.5 * scaleddistance);
  }
}