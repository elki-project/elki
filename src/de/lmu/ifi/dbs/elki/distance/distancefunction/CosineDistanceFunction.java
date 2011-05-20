package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Cosine distance function for feature vectors.
 * 
 * The cosine distance is computed from the cosine similarity by
 * <code>1-(cosine similarity)</code>.
 * 
 * @author Arthur Zimek
 */
public class CosineDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?,?>, DoubleDistance> {
  /**
   * Static instance
   */
  public static final CosineDistanceFunction STATIC = new CosineDistanceFunction();
  
  /**
   * Provides a CosineDistanceFunction.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public CosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed from the cosine similarity by
   * <code>1-(cosine similarity)</code>.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  @Override
  public DoubleDistance distance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    if (v1 instanceof SparseNumberVector<?,?> && v2 instanceof SparseNumberVector<?, ?>) {
      return sparseDistance((SparseNumberVector<?,?>)v1, (SparseNumberVector<?,?>)v2);
    }
    Vector m1 = v1.getColumnVector();
    m1.normalize();
    Vector m2 = v2.getColumnVector();
    m2.normalize();

    double d = 1 - m1.transposeTimes(m2);
    if(d < 0) {
      d = 0;
    }
    return new DoubleDistance(d);
  }

  /**
   * Compute the cosine distance for sparse vectors.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @return Cosine distance
   */
  private DoubleDistance sparseDistance(SparseNumberVector<?, ?> v1, SparseNumberVector<?, ?> v2) {
    BitSet b1 = v1.getNotNullMask();
    BitSet b2 = v2.getNotNullMask();
    BitSet both = (BitSet) b1.clone();
    both.and(b2);
    
    // Length of first vector
    double l1 = 0.0;
    for (int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i+1)) {
      final double val = v1.doubleValue(i);
      l1 += val * val;
    }
    l1 = Math.sqrt(l1);
    
    // Length of second vector
    double l2 = 0.0;
    for (int i = b2.nextSetBit(0); i >= 0; i = b2.nextSetBit(i+1)) {
      final double val = v2.doubleValue(i);
      l2 += val * val;
    }
    l2 = Math.sqrt(l2);
    
    // Cross product
    double cross = 0.0;
    for (int i = both.nextSetBit(0); i >= 0; i = both.nextSetBit(i+1)) {
      cross += v1.doubleValue(i) * v2.doubleValue(i);
    }
    
    return new DoubleDistance(1. - cross / (l1*l2));
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
    return "CosineDistance";
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
    protected CosineDistanceFunction makeInstance() {
      return CosineDistanceFunction.STATIC;
    }
  }
}