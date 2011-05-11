package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Maximum distance function to compute the Maximum distance for a pair of
 * FeatureVectors.
 * 
 * @author Erich Schubert
 */
public class MaximumDistanceFunction extends LPNormDistanceFunction implements RawDoubleDistance<NumberVector<?,?>> {
  /**
   * Static instance.
   */
  public static final MaximumDistanceFunction STATIC = new MaximumDistanceFunction();

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
  
  @Override
  public DoubleDistance distance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    return new DoubleDistance(doubleDistance(v1, v2));
  }
  
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double max = 0;
    for(int i = 1; i <= dim1; i++) {
      final double d = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      max = Math.max(d, max);
    }
    return max;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MaximumDistanceFunction makeInstance() {
      return MaximumDistanceFunction.STATIC;
    }
  }
}