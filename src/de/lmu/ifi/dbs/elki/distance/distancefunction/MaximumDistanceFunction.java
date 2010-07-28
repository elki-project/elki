package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Maximum distance function to compute the Maximum distance for a pair of
 * FeatureVectors.
 * 
 * @author Erich Schubert
 */
public class MaximumDistanceFunction extends LPNormDistanceFunction implements RawDoubleDistance<NumberVector<?,?>> {
  /**
   * Provides a Maximum distance function that can compute the Manhattan
   * distance (that is a DoubleDistance) for FeatureVectors.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public MaximumDistanceFunction() {
    super(Double.POSITIVE_INFINITY);
  }
  
  /**
   * Static instance.
   */
  public static final MaximumDistanceFunction STATIC = new MaximumDistanceFunction();

  /**
   * Factory method for {@link Parameterizable}
   * 
   * Note: we need this method, to override the parent class' method.
   * 
   * @param <V> Vector type
   * @param config Parameterization
   * @return Distance function
   */
  public static MaximumDistanceFunction parameterize(Parameterization config) {
    return new MaximumDistanceFunction();
  }

  @Override
  public DoubleDistance distance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double max = 0;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      double d = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      max = Math.max(d, max);
    }
    return new DoubleDistance(max);
  }
  
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double max = 0;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      double d = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      max = Math.max(d, max);
    }
    return max;
  }

  @Override
  public boolean isMetric() {
    return true;
  }
}