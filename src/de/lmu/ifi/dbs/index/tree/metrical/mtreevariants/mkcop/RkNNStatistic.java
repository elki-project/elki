package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.mkcop;

/**
 * Provides some statistics about performed reverse nn queries.
 *
 * @author Elke Achtert
 */
public class RkNNStatistic extends de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.util.RkNNStatistic {
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
