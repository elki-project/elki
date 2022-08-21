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
package elki.evaluation.scores.adapter;

import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDListIter;
import elki.evaluation.scores.ScoreEvaluation;

/**
 * This adapter is used to process a list of (double, DBID) objects. The list
 * <em>must</em> be sorted appropriately, the score is only used to detect
 * ties.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - DoubleDBIDListIter
 */
public class DistanceResultAdapter implements ScoreEvaluation.Adapter {
  /**
   * Set of positive examples.
   */
  private DBIDs set;

  /**
   * Original iterator
   */
  protected DoubleDBIDListIter iter;

  /**
   * Number of IDs
   */
  private int size;

  /**
   * Distance of previous.
   */
  protected double prevDist = Double.NaN;

  /**
   * Constructor
   * 
   * @param set Set of positive examples
   * @param iter Iterator for distance results
   * @param size Number of IDs
   */
  public DistanceResultAdapter(DBIDs set, DoubleDBIDListIter iter, int size) {
    super();
    this.set = set;
    this.iter = iter;
    this.size = size;
  }

  @Override
  public boolean valid() {
    return iter.valid();
  }

  @Override
  public DistanceResultAdapter advance() {
    prevDist = iter.doubleValue();
    iter.advance();
    return this;
  }

  @Override
  public boolean tiedToPrevious() {
    return iter.doubleValue() == prevDist;
  }

  @Override
  public int numPositive() {
    return set.size();
  }

  @Override
  public int numTotal() {
    return size;
  }

  @Override
  public boolean test() {
    return set.contains(iter);
  }
}
