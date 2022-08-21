/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.evaluation.scores;

import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDList;
import elki.evaluation.scores.adapter.DistanceResultAdapter;
import elki.utilities.datastructures.iterator.Iter;

/**
 * Compute ranking/scoring based evaluation measures.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - Adapter
 * @assoc - - - DoubleDBIDList
 */
public interface ScoreEvaluation {
  /**
   * Evaluate a given predicate and iterator.
   *
   * @param adapter Input data adapter
   * @return Score
   */
  double evaluate(Adapter adapter);

  /**
   * Evaluate given a list of positives and a scoring.
   *
   * @param ids Positive IDs, usually a set.
   * @param nei Query Result
   * @return Score
   */
  default double evaluate(DBIDs ids, DoubleDBIDList nei) {
    return evaluate(new DistanceResultAdapter(ids, nei.iter(), nei.size()));
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
   * Predicate to test whether an object is a true positive or false positive.
   *
   * @author Erich Schubert
   */
  interface Adapter extends Iter {
    /**
     * True if the current object is a positive example.
     *
     * @return {@code true} when positive.
     */
    boolean test();

    /**
     * Test whether the score is the same as the previous objects score.
     *
     * When there is no previous result, implementations should return false!
     *
     * @return Boolean
     */
    boolean tiedToPrevious();

    /**
     * Return the number of positive ids.
     *
     * @return Number of positive elements
     */
    int numPositive();

    /**
     * Return the number of all ids.
     *
     * @return Number of all elements
     */
    int numTotal();
  }
}
