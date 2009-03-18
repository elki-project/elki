package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Quadratic weight function, scaled using the maximum to reach 0.1 at that point.
 * 
 * 1.0 - 0.9 * (distance/max)^2
 * 
 * @author Erich Schubert
 */
public final class QuadraticWeight implements WeightFunction {
  /**
   * Evaluate quadratic weight. stddev is ignored.
   */
  public double getWeight(double distance, double max, @SuppressWarnings("unused") double stddev) {
    if (max <= 0) return 1.0;
    double relativedistance = distance / max;
    return 1.0 - 0.9 * relativedistance * relativedistance;
  }
}
