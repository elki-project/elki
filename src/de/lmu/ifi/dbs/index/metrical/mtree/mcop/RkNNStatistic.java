package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

/**
 * Provides some statistics about performed reverse nn queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RkNNStatistic extends de.lmu.ifi.dbs.index.metrical.mtree.mknn.ReversekNNStatistic {
  /**
   * The number of true hits.
   */
  public int numberTrueHits;

  /**
   * Clears the values of this statistic.
   */
  public void clear() {
    super.clear();
    this.numberTrueHits = 0;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return /**
   *         Returns a string representation of the object.
   */
  public String toString() {
    return "noResults = " + numberResults +
           "\nnoTrueHits = " + numberTrueHits +
           "\nnoCandidates = " + numberCandidates;
  }
}
