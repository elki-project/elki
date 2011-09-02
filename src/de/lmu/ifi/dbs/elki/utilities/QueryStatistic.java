package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


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
