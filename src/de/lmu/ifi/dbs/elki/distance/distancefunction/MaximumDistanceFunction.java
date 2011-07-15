package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Maximum distance function to compute the Maximum distance for a pair of
 * FeatureVectors.
 * 
 * @author Erich Schubert
 */
public class MaximumDistanceFunction extends LPNormDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
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
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects.");
    }
    double max = 0;
    for(int i = 1; i <= dim1; i++) {
      final double d;
      if(mbr1.getMax(i) < mbr2.getMin(i)) {
        d = mbr2.getMin(i) - mbr1.getMin(i);
      }
      else if(mbr1.getMin(i) > mbr2.getMax(i)) {
        d = mbr1.getMin(i) - mbr2.getMax(i);
      }
      else {
        // The object overlap in this dimension.
        continue;
      }
      max = Math.max(d, max);
    }
    return max;
  }

  @Override
  public double doubleCenterDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects.");
    }
    double max = 0;
    for(int i = 1; i <= dim1; i++) {
      final double c1 = (mbr1.getMax(i) - mbr1.getMin(i)) / 2;
      final double c2 = (mbr2.getMax(i) - mbr2.getMin(i)) / 2;
      final double d = Math.abs(c2 - c1);
      max = Math.max(d, max);
    }
    return max;
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
  public boolean isMetric() {
    return true;
  }

  @Override
  public <T extends NumberVector<?, ?>> SpatialDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
  }

  @Override
  public String toString() {
    return "MaximumDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
   if (this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
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