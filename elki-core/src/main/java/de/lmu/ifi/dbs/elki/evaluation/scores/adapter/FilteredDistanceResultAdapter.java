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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;

/**
 * This adapter is used to process a list of (double, DBID) objects, but allows
 * skipping one object in the ranking. The list <em>must</em> be sorted
 * appropriately, the score is only used to detect ties.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FilteredDistanceResultAdapter extends DistanceResultAdapter {
  /**
   * DBID to skip (usually: query object).
   */
  DBIDRef skip;

  /**
   * Constructor
   * 
   * @param iter Iterator for distance results
   * @param skip DBID to skip (reference must remain stable!)
   */
  public FilteredDistanceResultAdapter(DoubleDBIDListIter iter, DBIDRef skip) {
    super(iter);
    this.skip = skip;
    if(iter.valid() && DBIDUtil.equal(iter, skip)) {
      iter.advance();
    }
  }

  @Override
  public DistanceResultAdapter advance() {
    super.advance();
    if(iter.valid() && DBIDUtil.equal(iter, skip)) {
      iter.advance();
    }
    return this;
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