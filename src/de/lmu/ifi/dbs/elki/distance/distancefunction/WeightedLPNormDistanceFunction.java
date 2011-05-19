package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Weighted version of the Euclidean distance function.
 * 
 * @author Erich Schubert
 */
// TODO: make parameterizable; add optimized variants
public class WeightedLPNormDistanceFunction extends LPNormDistanceFunction {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   * 
   * @param p p value
   * @param weights Weight vector
   */
  public WeightedLPNormDistanceFunction(double p, double[] weights) {
    super(p);
    this.weights = weights;
  }

  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim = weights.length;
    if(dim != v1.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality of FeatureVector doesn't match weights!");
    }
    if(dim != v2.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality of FeatureVector doesn't match weights!");
    }

    final double p = getP();
    double sqrDist = 0;
    for(int i = 1; i <= dim; i++) {
      final double delta = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      sqrDist += Math.pow(delta, p) * weights[i - 1];
    }
    return Math.pow(sqrDist, 1.0 / p);
  }
}
