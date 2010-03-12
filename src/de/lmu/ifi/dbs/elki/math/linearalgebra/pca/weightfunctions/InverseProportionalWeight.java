package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Inverse proportional weight function, scaled using the maximum.
 * 
 * 1 / (1 + distance/max)
 * 
 * @author Erich Schubert
 */
public final class InverseProportionalWeight implements WeightFunction {
  /**
   * Get inverse proportional weight. stddev is ignored.
   */
  public double getWeight(double distance, double max, @SuppressWarnings("unused") double stddev) {
    if(max <= 0) {
      return 1.0;
    }
    double relativedistance = distance / max;
    return 1 / (1 + 9 * relativedistance);
  }
}