package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.Predicate;

/**
 * Test predicate using a DBID set as positive elements.
 * 
 * @apiviz.composedOf DBIDs
 *
 * @author Erich Schubert
 */
public class DBIDsTest implements Predicate<DBIDRefIter> {
  /**
   * DBID set.
   */
  private DBIDs set;

  /**
   * Constructor.
   * 
   * @param set Set of positive objects
   */
  public DBIDsTest(DBIDs set) {
    this.set = set;
  }

  @Override
  public boolean test(DBIDRefIter o) {
    return set.contains(o.getRef());
  }

  @Override
  public int numPositive() {
    return set.size();
  }
}