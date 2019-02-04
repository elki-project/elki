/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.evaluation.scores;

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DBIDsTest;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DistanceResultAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;

/**
 * Compute ranking/scoring based evaluation measures.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - ScoreIter
 * @assoc - - - Predicate
 * @assoc - - - DoubleDBIDList
 * @assoc - - - Cluster
 * @assoc - - - DBIDs
 * @assoc - - - OutlierResult
 */
public interface ScoreEvaluation {
  /**
   * Evaluate a given predicate and iterator.
   *
   * @param predicate Predicate (for positives)
   * @param iter Iterator
   * @return Score
   * @param <I> Iterator type
   */
  <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter);

  /**
   * Evaluate given a list of positives and a scoring.
   *
   * @param ids Positive IDs, usually a set.
   * @param nei Query Result
   * @return Score
   */
  default double evaluate(DBIDs ids, DoubleDBIDList nei) {
    return evaluate(new DBIDsTest(DBIDUtil.ensureSet(ids)), new DistanceResultAdapter(nei.iter()));
  }

  /**
   * Expected score for a random result.
   *
   * @param pos Number of positive elements
   * @param all Total number of elements
   * @return Expected score
   */
  double expected(int pos, int all);

  /**
   * Iterator for comparing scores.
   *
   * @author Erich Schubert
   */
  interface ScoreIter extends Iter {
    /**
     * Test whether the score is the same as the previous objects score.
     *
     * When there is no previous result, implementations should return false!
     *
     * @return Boolean
     */
    boolean tiedToPrevious();
  }

  /**
   * Predicate to test whether an object is a true positive or false positive.
   *
   * @author Erich Schubert
   *
   * @param <T> Data type
   */
  interface Predicate<T> {
    /**
     * Test a result.
     *
     * @param o Object to test
     * @return {@code true} when positive.
     */
    boolean test(T o);

    /**
     * Return the number of positive ids.
     *
     * @return Number of positive elements
     */
    int numPositive();
  }
}
