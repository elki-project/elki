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
package de.lmu.ifi.dbs.elki.evaluation.scores.adapter;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;

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
public class DistanceResultAdapter implements ScoreIter, DBIDRefIter {
  /**
   * Original Iterator
   */
  protected DoubleDBIDListIter iter;

  /**
   * Distance of previous.
   */
  protected double prevDist = Double.NaN;

  /**
   * Constructor
   * 
   * @param iter Iterator for distance results
   */
  public DistanceResultAdapter(DoubleDBIDListIter iter) {
    super();
    this.iter = iter;
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
  public DBIDRef getRef() {
    return iter;
  }

  @Override
  public boolean tiedToPrevious() {
    return iter.doubleValue() == prevDist;
  }

  @Deprecated
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Deprecated
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}