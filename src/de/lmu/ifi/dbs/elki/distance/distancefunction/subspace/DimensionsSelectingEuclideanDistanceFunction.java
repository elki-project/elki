package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Provides a distance function that computes the Euclidean distance between
 * feature vectors only in specified dimensions.
 * 
 * @author Elke Achtert
 */
public class DimensionsSelectingEuclideanDistanceFunction extends AbstractDimensionsSelectingDoubleDistanceFunction<NumberVector<?, ?>> implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * Constructor.
   * 
   * @param dimensions Selected dimensions
   */
  public DimensionsSelectingEuclideanDistanceFunction(BitSet dimensions) {
    super(dimensions);
  }

  /**
   * Provides the Euclidean distance between two given feature vectors in the
   * selected dimensions.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Euclidean distance between two given feature vectors in the
   *         selected dimensions
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  " + "first argument: " + v1 + "\n  " + "second argument: " + v2);
    }

    if(v1.getDimensionality() < getSelectedDimensions().cardinality()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " + "is not consistent with the specified dimensions " + "to be considered for distance computation.\n  " + "dimensionality of the feature space: " + v1.getDimensionality() + "\n  " + "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for(int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
      double manhattanI = v1.doubleValue(d + 1) - v2.doubleValue(d + 1);
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

  protected double doubleMinDistObject(SpatialComparable mbr, NumberVector<?, ?> v) {
    if(mbr.getDimensionality() != v.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
    }
    if(v.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " + "is not consistent with the specified dimensions " + "to be considered for distance computation.\n  " + "dimensionality of the feature space: " + v.getDimensionality() + "\n  " + "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for(int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
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

      double manhattanI = value - r;
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    if(mbr1.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " + "is not consistent with the specified dimensions " + "to be considered for distance computation.\n  " + "dimensionality of the feature space: " + mbr1.getDimensionality() + "\n  " + "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for(int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
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
      double manhattanI = m1 - m2;
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

  @Override
  public double doubleCenterDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
    }
    if(mbr1.getDimensionality() < getSelectedDimensions().size()) {
      throw new IllegalArgumentException("The dimensionality of the feature space " + "is not consistent with the specified dimensions " + "to be considered for distance computation.\n  " + "dimensionality of the feature space: " + mbr1.getDimensionality() + "\n  " + "specified dimensions: " + getSelectedDimensions());
    }

    double sqrDist = 0;
    for(int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
      double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
      double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;

      double manhattanI = c1 - c2;
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
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
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public <T extends NumberVector<?, ?>> SpatialPrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(database, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDimensionsSelectingDoubleDistanceFunction.Parameterizer {
    @Override
    protected DimensionsSelectingEuclideanDistanceFunction makeInstance() {
      return new DimensionsSelectingEuclideanDistanceFunction(dimensions);
    }
  }
}