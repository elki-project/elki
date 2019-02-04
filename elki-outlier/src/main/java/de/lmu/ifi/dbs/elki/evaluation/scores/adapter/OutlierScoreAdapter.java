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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;

/**
 * This adapter can be used for an arbitrary collection of Integers, and uses
 * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
 * 
 * Note that of course, no id should occur more than once.
 * 
 * The ROC values would be incorrect then anyway!
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - OutlierResult
 */
public class OutlierScoreAdapter implements ScoreIter, DBIDRefIter {
  /**
   * Original iterator.
   */
  private DBIDIter iter;

  /**
   * Outlier score.
   */
  private DoubleRelation scores;

  /**
   * Previous value.
   */
  double prev = Double.NaN;

  /**
   * Constructor.
   * 
   * @param o Result
   */
  public OutlierScoreAdapter(OutlierResult o) {
    super();
    this.iter = o.getOrdering().order(o.getScores().getDBIDs()).iter();
    this.scores = o.getScores();
  }

  @Override
  public boolean valid() {
    return iter.valid();
  }

  @Override
  public OutlierScoreAdapter advance() {
    prev = scores.doubleValue(iter);
    iter.advance();
    return this;
  }

  @Override
  public boolean tiedToPrevious() {
    return scores.doubleValue(iter) == prev;
  }

  @Override
  public DBIDRef getRef() {
    return iter;
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