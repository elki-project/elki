package de.lmu.ifi.dbs.index.metrical.mtree.mknn;

/**
 * Provides some statistics about performed reverse nn queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RkNNStatistic {
  /**
   * The number of overall result;
   */
  public int noResults;

  /**
   * The number of candidates.
   */
  public int noCandidates;

  /**
   * Clears the values of this statistic.
   */
  public void clear() {
    this.noResults = 0;
    this.noCandidates = 0;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return /**
   *         Returns a string representation of the object.
   */
  public String toString() {
    return "noResults = " + noResults +
           "\nnoCandidates = " + noCandidates;
  }
}
