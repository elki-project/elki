package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Provides the squared Euclidean distance for FeatureVectors. This results in
 * the same rankings, but saves computing the square root as often.
 * 
 * @author Arthur Zimek
 */
public class SquaredEuclideanDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * Static instance. Use this!
   */
  public static final SquaredEuclideanDistanceFunction STATIC = new SquaredEuclideanDistanceFunction();

  /**
   * Provides a Euclidean distance function that can compute the Euclidean
   * distance (that is a DoubleDistance) for FeatureVectors.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public SquaredEuclideanDistanceFunction() {
    super();
  }

  /**
   * Provides the squared Euclidean distance between the given two vectors.
   * 
   * @return the squared Euclidean distance between the given two vectors as raw
   *         double value
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double sqrDist = 0;
    for(int i = 1; i <= dim1; i++) {
      final double delta = v1.doubleValue(i) - v2.doubleValue(i);
      sqrDist += delta * delta;
    }
    return sqrDist;
  }

  protected double doubleMinDistObject(SpatialComparable mbr, NumberVector<?, ?> v) {
    final int dim = mbr.getDimensionality();
    if(dim != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString() + "\n" + dim + "!=" + v.getDimensionality());
    }

    double sqrDist = 0;
    for(int d = 1; d <= dim; d++) {
      double value = v.doubleValue(d);
      double r;
      if(value < mbr.getMin(d)) {
        r = mbr.getMin(d);
      }
      else if(value > mbr.getMax(d)) {
        r = mbr.getMax(d);
      }
      else {
        r = value;
      }

      final double manhattanI = value - r;
      sqrDist += manhattanI * manhattanI;
    }
    return sqrDist;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Some optimizations for simpler cases.
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?, ?>) mbr1, (NumberVector<?, ?>) mbr2);
      }
      else {
        return doubleMinDistObject(mbr2, (NumberVector<?, ?>) mbr1);
      }
    }
    else if(mbr2 instanceof NumberVector) {
      return doubleMinDistObject(mbr1, (NumberVector<?, ?>) mbr2);
    }
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }

    double sqrDist = 0;
    for(int d = 1; d <= dim1; d++) {
      final double m1, m2;
      if(mbr1.getMax(d) < mbr2.getMin(d)) {
        m1 = mbr1.getMax(d);
        m2 = mbr2.getMin(d);
      }
      else if(mbr1.getMin(d) > mbr2.getMax(d)) {
        m1 = mbr1.getMin(d);
        m2 = mbr2.getMax(d);
      }
      else { // The mbrs intersect!
        continue;
      }
      final double manhattanI = m1 - m2;
      sqrDist += manhattanI * manhattanI;
    }
    return sqrDist;
  }

  @Override
  public double doubleCenterDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality();
    if(dim1 != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
  
    double sqrDist = 0;
    for(int d = 1; d <= dim1; d++) {
      final double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
      final double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;
  
      final double manhattanI = c1 - c2;
      sqrDist += manhattanI * manhattanI;
    }
    return sqrDist;
  }
  
  @Override
  public DoubleDistance distance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    return new DoubleDistance(doubleDistance(o1, o2));
  }

  @Override
  public DoubleDistance centerDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleCenterDistance(mbr1, mbr2));
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public <T extends NumberVector<?, ?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
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
    protected SquaredEuclideanDistanceFunction makeInstance() {
      return SquaredEuclideanDistanceFunction.STATIC;
    }
  }
}