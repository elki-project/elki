package de.lmu.ifi.dbs.elki.utilities;

/**
 * Provides some statistics about queries
 * using a filter-refinement architecture.
 *
 * @author Elke Achtert
 */
public class QueryStatistic {
    /**
     * The number of candidate objects which need a refinement.
     */
    private int candidates;

    /**
     * The number of true hits which do not need a refinement.
     */
    private int trueHits;

    /**
     * The overall number of the result objects after refinement
     * plus the true hits.
     */
    private int results;

    /**
     * Clears the values of this statistic.
     */
    public void clear() {
        this.candidates = 0;
        this.trueHits = 0;
        this.results = 0;
    }

    /**
     * Adds the specified number to the number of the result objects.
     *
     * @param results the number of the result objects to be added
     */
    public void addResults(int results) {
        this.results += results;
    }

    /**
     * Adds the specified number to the number of the candidate objects.
     *
     * @param candidates the number of the candidate objects to be added
     */
    public void addCandidates(int candidates) {
        this.candidates += candidates;
    }

    /**
     * Adds the specified number to the number of the true hits.
     *
     * @param trueHits the number of the true hits to be added
     */
    public void addTrueHits(int trueHits) {
        this.trueHits += trueHits;
    }

    /**
     * Returns the number of candidate objects and the number of the result objects after refinement.
     *
     * @return a string representation of this query statistic
     */
    @Override
    public String toString() {
        return
            "# candidates = " + candidates +
                "\n# true hits  = " + trueHits +
                "\n# results    = " + results;
    }
}
