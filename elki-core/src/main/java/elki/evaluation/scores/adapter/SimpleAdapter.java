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

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.evaluation.scores.ScoreEvaluation;

/**
 * This adapter can be used for an arbitrary collection of DBIDs, and does hence
 * not have a notion of ties.
 * <p>
 * Note that of course, no id should occur more than once.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - DBIDIter
 */
public class SimpleAdapter implements ScoreEvaluation.Adapter {
  /**
   * Set of positive examples.
   */
  private DBIDs set;

  /**
   * Original Iterator
   */
  private DBIDIter iter;

  /**
   * Number of IDs
   */
  private int size;

  /**
   * Constructor
   * 
   * @param set Set of positive examples
   * @param iter Iterator for object IDs
   * @param size Number of IDs
   */
  public SimpleAdapter(DBIDs set, DBIDIter iter, int size) {
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
  public SimpleAdapter advance() {
    iter.advance();
    return this;
  }

  @Override
  public boolean tiedToPrevious() {
    return false; // No information.
  }

  @Override
  public boolean test() {
    return set.contains(iter);
  }

  @Override
  public int numPositive() {
    return set.size();
  }

  @Override
  public int numTotal() {
    return size;
  }
}
