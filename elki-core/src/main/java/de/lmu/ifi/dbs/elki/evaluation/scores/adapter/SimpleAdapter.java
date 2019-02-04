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
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.ScoreIter;

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
 * @composed - - - DBIDIter
 */
public class SimpleAdapter implements ScoreIter, DBIDRefIter {
  /**
   * Original Iterator
   */
  private DBIDIter iter;

  /**
   * Constructor
   * 
   * @param iter Iterator for object IDs
   */
  public SimpleAdapter(DBIDIter iter) {
    super();
    this.iter = iter;
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

  @Override
  public DBIDRef getRef() {
    return iter;
  }
}