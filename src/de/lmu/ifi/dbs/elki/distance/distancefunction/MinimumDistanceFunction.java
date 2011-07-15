package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Maximum distance function to compute the Minimum distance for a pair of
 * FeatureVectors.
 * 
 * @author Erich Schubert
 */
// TODO: add spatial?
public class MinimumDistanceFunction extends AbstractVectorDoubleDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
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
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = mbr1.getDimensionality();
    if(dim != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
    }
    double min = Double.MAX_VALUE;
    for(int i = 1; i <= dim; i++) {
      final double min1 = mbr1.getMin(i);
      final double max1 = mbr1.getMax(i);
      final double min2 = mbr2.getMin(i);
      final double max2 = mbr2.getMax(i);
      final double d;
      if(max1 <= min2) {
        d = min2 - max1;
      }
      else if(max2 <= min1) {
        d = min1 - max2;
      }
      else {
        // Overlap in this dimension
        min = 0.0;
        break;
      }
      min = Math.min(d, min);
    }
    return min;
  }

  @Override
  public double doubleCenterDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = mbr1.getDimensionality();
    if(dim != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
    }
    double min = Double.MAX_VALUE;
    for(int i = 1; i <= dim; i++) {
      final double d = Math.abs((mbr1.getMin(i) + mbr1.getMax(i)) / 2 - (mbr2.getMin(i) + mbr1.getMax(i)) / 2);
      min = Math.min(d, min);
    }
    return min;
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public DoubleDistance centerDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleCenterDistance(mbr1, mbr2));
  }

  @Override
  public String toString() {
    return "MinimumDistance";
  }

  @Override
  public <T extends NumberVector<?, ?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    return this.getClass().equals(obj.getClass());
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