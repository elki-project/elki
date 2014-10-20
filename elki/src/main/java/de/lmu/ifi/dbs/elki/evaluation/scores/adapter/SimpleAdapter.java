package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;

/**
 * This adapter can be used for an arbitrary collection of Integers, and uses
 * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
 * 
 * Note that of course, no id should occur more than once.
 * 
 * The ROC values would be incorrect then anyway!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DBIDIter
 */
public class SimpleAdapter implements ScoreIter, DBIDRefIter {
  /**
   * Original Iterator
   */
  private DBIDIter iter;

  /**
   * Constructor
   * 
   * @param iter Iterator for object IDs
   */
  public SimpleAdapter(DBIDIter iter) {
    super();
    this.iter = iter;
  }

  @Override
  public boolean valid() {
    return iter.valid();
  }

  @Override
  public SimpleAdapter advance() {
    iter.advance();
    return this;
  }

  @Override
  public boolean tiedToPrevious() {
    return false; // No information.
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

  @Override
  public DBIDRef getRef() {
    return iter;
  }
}