package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Manhattan distance function to compute the Manhattan distance for a pair of
 * FeatureVectors.
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public class ManhattanDistanceFunction<V extends FeatureVector<V, ?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * Provides a Manhattan distance function that can compute the Manhattan
   * distance (that is a DoubleDistance) for FeatureVectors.
   */
  public ManhattanDistanceFunction() {
    super();
  }

  public DoubleDistance distance(V v1, V v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double sum = 0;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      sum += Math.abs(v1.getValue(i).doubleValue() - v2.getValue(i).doubleValue());
    }
    return new DoubleDistance(sum);
  }
}
