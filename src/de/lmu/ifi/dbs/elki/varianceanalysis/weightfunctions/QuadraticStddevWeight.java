package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class QuadraticStddevWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    assert (stddev > 0);
    // A scaling of 3 was picked arbitrarily
    double scaleddistance = distance / stddev / 3;
    // After this, the result would be negative.
    if(scaleddistance >= 1.0)
      return 0.0;
    return 1.0 - scaleddistance * scaleddistance;
  }
}
