/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2024
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
package elki.database.query.range;

import elki.database.ids.DoubleDBIDListMIter;
import elki.database.ids.ModifiableDoubleDBIDList;

/**
 * Wrapper class to allow using an Euclidean search tree with squared Euclidean
 * distance.
 * 
 * @author Erich Schubert
 *
 * @param <O> Data type
 */
public class SquaredRangeSearcher<O> implements RangeSearcher<O> {
  /**
   * Wrapped searcher
   */
  private RangeSearcher<O> inner;

  /**
   * Constructor.
   *
   * @param inner Inner searcher
   */
  public SquaredRangeSearcher(RangeSearcher<O> inner) {
    this.inner = inner;
  }

  @Override
  public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
    int oldsize = result.size();
    inner.getRange(query, Math.sqrt(range), result);
    for(DoubleDBIDListMIter iter = result.iter().seek(oldsize); iter.valid(); iter.advance()) {
      double d = iter.doubleValue();
      iter.setDouble(d * d);
    }
    return result;
  }
}
