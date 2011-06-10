package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Abstract base class for Cosine and ArcCosine distances.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractCosineDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Constructor.
   */
  public AbstractCosineDistanceFunction() {
    super();
  }
  
  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  protected double angle(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1 instanceof SparseNumberVector<?, ?> && v2 instanceof SparseNumberVector<?, ?>) {
      return angleSparse((SparseNumberVector<?, ?>) v1, (SparseNumberVector<?, ?>) v2);
    }
    Vector m1 = v1.getColumnVector();
    m1.normalize();
    Vector m2 = v2.getColumnVector();
    m2.normalize();
    return m1.transposeTimes(m2);
  }

  /**
   * Compute the angle for sparse vectors.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @return angle
   */
  protected double angleSparse(SparseNumberVector<?, ?> v1, SparseNumberVector<?, ?> v2) {
    BitSet b1 = v1.getNotNullMask();
    BitSet b2 = v2.getNotNullMask();
    BitSet both = (BitSet) b1.clone();
    both.and(b2);
  
    // Length of first vector
    double l1 = 0.0;
    for(int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i + 1)) {
      final double val = v1.doubleValue(i);
      l1 += val * val;
    }
    l1 = Math.sqrt(l1);
  
    // Length of second vector
    double l2 = 0.0;
    for(int i = b2.nextSetBit(0); i >= 0; i = b2.nextSetBit(i + 1)) {
      final double val = v2.doubleValue(i);
      l2 += val * val;
    }
    l2 = Math.sqrt(l2);
  
    // Cross product
    double cross = 0.0;
    for(int i = both.nextSetBit(0); i >= 0; i = both.nextSetBit(i + 1)) {
      cross += v1.doubleValue(i) * v2.doubleValue(i);
    }
    return cross / (l1 * l2);
  }

  @Override
  public <T extends NumberVector<?, ?>> PrimitiveDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new PrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
  }
}