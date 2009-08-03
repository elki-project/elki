package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Maximum distance function to compute the Minimum distance for a pair of
 * NumberVectors.
 * 
 * @author Erich Schubert
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class MinimumDistanceFunction<V extends NumberVector<V, ?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * Provides a Minimum distance function that can compute the Minimum
   * distance (that is a DoubleDistance) for FeatureVectors.
   */
  public MinimumDistanceFunction() {
    super();
  }

  public DoubleDistance distance(V v1, V v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of NumberVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double min = Double.MAX_VALUE;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      double d = Math.abs(v1.getValue(i).doubleValue() - v2.getValue(i).doubleValue());
      min = Math.min(d, min);
    }
    return new DoubleDistance(min);
  }
}
