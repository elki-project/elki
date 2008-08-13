package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class GaussStddevWeight implements WeightFunction {
  // Scaling was removed, since it will be just a constant factor in the PCA
  // anyway
  // private double scaling = 1 / Math.sqrt(2 * Math.PI);

  public double getWeight(double distance, double max, double stddev) {
    assert (stddev > 0);
    double scaleddistance = distance / stddev;
    // scaling was removed, since it will be just a constant factor in the PCA
    // anyway
    return java.lang.Math.exp(-.5 * scaleddistance * scaleddistance);
  }
}
