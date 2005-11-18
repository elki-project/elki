package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

/**
 * Provides some statistics about performed reverse nn queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RkNNStatistic extends de.lmu.ifi.dbs.index.metrical.mtree.mknn.RkNNStatistic {
  /**
   * The number of true hits.
   */
  public int noTrueHits;

  /**
   * Clears the values of this statistic.
   */
  public void clear() {
    super.clear();
    this.noTrueHits = 0;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return /**
   *         Returns a string representation of the object.
   */
  public String toString() {
    return "noResults = " + noResults +
           "\nnoTrueHits = " + noTrueHits +
           "\nnoCandidates = " + noCandidates;
  }
}
