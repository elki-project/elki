package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.Predicate;

/**
 * Class that uses a NumberVector as reference, and considers all zero values as
 * positive entries.
 * 
 * @apiviz.composedOf NumberVector
 * 
 * @author Erich Schubert
 */
public class VectorZero implements Predicate<IncreasingVectorIter> {
  /**
   * Vector to use as reference
   */
  NumberVector vec;

  /**
   * Number of positive values.
   */
  int numpos;

  /**
   * Constructor.
   * 
   * @param vec Reference vector.
   */
  public VectorZero(NumberVector vec) {
    this.vec = vec;
    this.numpos = 0;
    for(int i = 0, l = vec.getDimensionality(); i < l; i++) {
      if(Math.abs(vec.doubleValue(i)) < Double.MIN_NORMAL) {
        ++numpos;
      }
    }
  }

  @Override
  public boolean test(IncreasingVectorIter o) {
    return Math.abs(vec.doubleValue(o.dim())) < Double.MIN_NORMAL;
  }

  @Override
  public int numPositive() {
    return numpos;
  }
}