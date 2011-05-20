package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Maximum distance function to compute the Minimum distance for a pair of
 * FeatureVectors.
 * 
 * @author Erich Schubert
 */
// TODO: add spatial?
public class MinimumDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements PrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * Static instance. Use this.
   */
  public static final MinimumDistanceFunction STATIC = new MinimumDistanceFunction();

  /**
   * Provides a Minimum distance function that can compute the Minimum distance
   * (that is a DoubleDistance) for FeatureVectors.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public MinimumDistanceFunction() {
    super();
  }

  @Override
  public DoubleDistance distance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim = v1.getDimensionality();
    if(dim != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double min = Double.MAX_VALUE;
    for(int i = 1; i <= dim; i++) {
      final double d = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      min = Math.min(d, min);
    }
    return new DoubleDistance(min);
  }

  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim = v1.getDimensionality();
    if(dim != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }
    double min = Double.MAX_VALUE;
    for(int i = 1; i <= dim; i++) {
      final double d = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      min = Math.min(d, min);
    }
    return min;
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public String toString() {
    return "MinimumDistance";
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
    protected MinimumDistanceFunction makeInstance() {
      return MinimumDistanceFunction.STATIC;
    }
  }
}