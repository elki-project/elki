package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Quadratic weight function, scaled using the maximum to reach 0.1 at that point.
 * 
 * 1.0 - 0.9 * (distance/max)^2
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class QuadraticWeight implements WeightFunction {
  /**
   * Evalue quadratic weight. stddev is ignored.
   */
  public double getWeight(double distance, double max, double stddev) {
    if (max <= 0) return 1.0;
    double relativedistance = distance / max;
    return 1.0 - 0.9 * relativedistance * relativedistance;
  }
}
