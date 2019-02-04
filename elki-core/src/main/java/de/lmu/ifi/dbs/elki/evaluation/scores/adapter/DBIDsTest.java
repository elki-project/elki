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

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation.Predicate;

/**
 * Test predicate using a DBID set as positive elements.
 * 
 * @composed - - - DBIDs
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DBIDsTest implements Predicate<DBIDRefIter> {
  /**
   * DBID set.
   */
  private DBIDs set;

  /**
   * Constructor.
   * 
   * @param set Set of positive objects
   */
  public DBIDsTest(DBIDs set) {
    this.set = set;
  }

  @Override
  public boolean test(DBIDRefIter o) {
    return set.contains(o.getRef());
  }

  @Override
  public int numPositive() {
    return set.size();
  }
}