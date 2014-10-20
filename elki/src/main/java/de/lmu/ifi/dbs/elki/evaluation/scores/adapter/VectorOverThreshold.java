package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.Predicate;

/**
 * Class that uses a NumberVector as reference, and considers all non-zero
 * values as positive entries.
 * 
 * @apiviz.composedOf NumberVector
 * 
 * @author Erich Schubert
 */
public class VectorOverThreshold implements Predicate<DecreasingVectorIter> {
  /**
   * Vector to use as reference
   */
  NumberVector vec;

  /**
   * Threshold
   */
  double threshold;

  /**
   * Number of positive values.
   */
  int numpos;

  /**
   * Constructor.
   * 
   * @param vec Reference vector.
   * @param threshold Threshold value.
   */
  public VectorOverThreshold(NumberVector vec, double threshold) {
    super();
    this.vec = vec;
    this.threshold = threshold;
    this.numpos = 0;
    for(int i = 0, l = vec.getDimensionality(); i < l; i++) {
      if(vec.doubleValue(i) > threshold) {
        ++numpos;
      }
    }
  }

  @Override
  public boolean test(DecreasingVectorIter o) {
    return Math.abs(vec.doubleValue(o.dim())) > threshold;
  }

  @Override
  public int numPositive() {
    return numpos;
  }
}