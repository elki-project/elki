package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Euclidean distance function. Optimized for sparse vectors.
 * 
 * @author Erich Schubert
 */
public class SparseEuclideanDistanceFunction extends SparseLPNormDistanceFunction {
  /**
   * Static instance
   */
  public static final SparseEuclideanDistanceFunction STATIC = new SparseEuclideanDistanceFunction();
  
  /**
   * Constructor.
   * 
   * @deprecated Use static instance instead.
   */
  @Deprecated
  public SparseEuclideanDistanceFunction() {
    super(2.0);
  }

  @Override
  public double doubleDistance(SparseNumberVector<?, ?> v1, SparseNumberVector<?, ?> v2) {
    // Get the bit masks
    BitSet b1 = v1.getNotNullMask();
    BitSet b2 = v2.getNotNullMask();
    double sqrDist = 0;
    int i1 = b1.nextSetBit(0);
    int i2 = b2.nextSetBit(0);
    while(i1 >= 0 && i2 >= 0) {
      if(i1 == i2) {
        // Set in both
        double manhattanI = v1.doubleValue(i1) - v2.doubleValue(i2);
        sqrDist += manhattanI * manhattanI;
        i1 = b1.nextSetBit(i1 + 1);
        i2 = b2.nextSetBit(i2 + 1);
      }
      else if(i1 < i2 && i1 >= 0) {
        // In first only
        double manhattanI = v1.doubleValue(i1);
        sqrDist += manhattanI * manhattanI;
        i1 = b1.nextSetBit(i1 + 1);
      }
      else {
        // In second only
        double manhattanI = v2.doubleValue(i2);
        sqrDist += manhattanI * manhattanI;
        i2 = b1.nextSetBit(i2 + 1);
      }
    }
    return Math.sqrt(sqrDist);
  }

  @Override
  public double doubleNorm(SparseNumberVector<?, ?> v1) {
    double sqrDist = 0;
    // Get the bit masks
    BitSet b1 = v1.getNotNullMask();
    // Set in first only
    for(int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i + 1)) {
      double manhattanI = v1.doubleValue(i);
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

  /**
   * Parameterizer
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SparseEuclideanDistanceFunction makeInstance() {
      return SparseEuclideanDistanceFunction.STATIC;
    }
  }
}