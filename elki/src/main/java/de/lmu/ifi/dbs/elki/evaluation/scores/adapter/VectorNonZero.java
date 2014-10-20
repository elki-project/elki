package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Class that uses a NumberVector as reference, and considers all non-zero
 * values as positive entries.
 * 
 * @apiviz.composedOf NumberVector
 * 
 * @author Erich Schubert
 */
public class VectorNonZero extends VectorOverThreshold {
  /**
   * Constructor.
   * 
   * @param vec Reference vector.
   */
  public VectorNonZero(NumberVector vec) {
    super(vec, 0.);
  }
}