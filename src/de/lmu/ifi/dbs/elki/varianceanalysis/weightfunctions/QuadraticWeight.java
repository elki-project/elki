package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public class QuadraticWeight implements WeightFunction {
  public double getWeight(double distance, double max, double stddev) {
    double relativedistance = distance / max;
    return 1.0 - 0.9 * relativedistance * relativedistance;
  }
}
