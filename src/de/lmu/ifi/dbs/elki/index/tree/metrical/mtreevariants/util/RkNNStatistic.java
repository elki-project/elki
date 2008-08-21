package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.util;

/**
 * Provides some statistics about performed reverse nn queries.
 *
 * @author Elke Achtert
 */
public class RkNNStatistic {
    /**
     * The number of overall result;
     */
    public int numberResults;

    /**
     * The number of candidates.
     */
    public int numberCandidates;

    /**
     * Clears the values of this statistic.
     */
    public void clear() {
        this.numberResults = 0;
        this.numberCandidates = 0;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return /** Returns a string representation of the object.
     */
    public String toString() {
        return "noResults = " + numberResults + "\nnoCandidates = "
            + numberCandidates;
    }
}
