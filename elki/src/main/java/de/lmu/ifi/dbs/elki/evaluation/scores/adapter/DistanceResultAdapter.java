package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;

/**
 * This adapter is used to process a list of (double, DBID) objects. The list
 * <em>must</em> be sorted appropriately, the score is only used to detect
 * ties.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DoubleDBIDListIter
 */
public class DistanceResultAdapter implements ScoreIter, DBIDRefIter {
  /**
   * Original Iterator
   */
  protected DoubleDBIDListIter iter;

  /**
   * Distance of previous.
   */
  protected double prevDist = Double.NaN;

  /**
   * Constructor
   * 
   * @param iter Iterator for distance results
   */
  public DistanceResultAdapter(DoubleDBIDListIter iter) {
    super();
    this.iter = iter;
  }

  @Override
  public boolean valid() {
    return iter.valid();
  }

  @Override
  public DistanceResultAdapter advance() {
    prevDist = iter.doubleValue();
    iter.advance();
    return this;
  }

  @Override
  public DBIDRef getRef() {
    return iter;
  }

  @Override
  public boolean tiedToPrevious() {
    return iter.doubleValue() == prevDist;
  }

  @Deprecated
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Deprecated
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}