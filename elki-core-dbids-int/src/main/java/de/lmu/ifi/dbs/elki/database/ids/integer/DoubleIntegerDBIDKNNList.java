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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;

/**
 * kNN list, but without automatic sorting. Use with care, as others may expect
 * the results to be sorted!
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
class DoubleIntegerDBIDKNNList extends DoubleIntegerDBIDArrayList implements IntegerDBIDKNNList {
  /**
   * The k value this list was generated for.
   */
  final int k;

  /**
   * Constructor.
   */
  public DoubleIntegerDBIDKNNList() {
    super();
    this.k = -1;
  }

  /**
   * Constructor.
   *
   * @param k K parameter
   * @param size Actual size
   */
  public DoubleIntegerDBIDKNNList(final int k, int size) {
    super(size);
    this.k = k;
  }

  @Override
  public int getK() {
    return k;
  }

  @Override
  public double getKNNDistance() {
    return (size >= k) ? dists[k - 1] : Double.POSITIVE_INFINITY;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(size() * 20 + 20).append("kNNList[");
    DoubleDBIDListIter iter = this.iter();
    if(iter.valid()) {
      buf.append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
    }
    while(iter.advance().valid()) {
      buf.append(',').append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
    }
    return buf.append(']').toString();
  }
}
