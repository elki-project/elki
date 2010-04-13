package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Manhattan distance function to compute the Manhattan distance for a pair of
 * FeatureVectors.
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public class ManhattanDistanceFunction<V extends NumberVector<V, ?>> extends LPNormDistanceFunction<V> implements RawDoubleDistance<V> {
  /**
   * Provides a Manhattan distance function that can compute the Manhattan
   * distance (that is a DoubleDistance) for FeatureVectors.
   */
  public ManhattanDistanceFunction() {
    super(1.0);
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * Note: we need this method, to override the parent class' method.
   * 
   * @param <V> Vector type
   * @param config Parameterization
   * @return Distance function
   */
  public static <V extends NumberVector<V, ?>> ManhattanDistanceFunction<V> parameterize(Parameterization config) {
    return new ManhattanDistanceFunction<V>();
  }

  @Override
  public DoubleDistance distance(V v1, V v2) {
    return new DoubleDistance(doubleDistance(v1, v2));
  }

  /**
   * Compute the Manhattan distance
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Manhattan distance value
   */
  @Override
  public double doubleDistance(V v1, V v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double sum = 0;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      sum += Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
    }
    return sum;
  }

  @Override
  public Class<? super V> getInputDatatype() {
    return NumberVector.class;
  }
}