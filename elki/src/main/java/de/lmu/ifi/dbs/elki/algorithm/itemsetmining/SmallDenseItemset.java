package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2016
 * Ludwig-Maximilians-Universität München
 * Lehr- und Forschungseinheit für Datenbanksysteme
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

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * APRIORI itemset, dense representation for up to 64 items.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SmallDenseItemset extends Itemset {
  /**
   * Items, as a bitmask.
   */
  long items;

  /**
   * Itemset length.
   */
  int length;

  /**
   * Constructor.
   * 
   * @param items Items
   * @param length Length (Cardinality of itemset)
   */
  public SmallDenseItemset(long items, int length) {
    this.items = items;
    this.length = length;
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public boolean containedIn(SparseNumberVector bv) {
    if(bv instanceof BitVector) {
      return ((BitVector) bv).contains(new long[] { items });
    }
    return super.containedIn(bv);
  }

  @Override
  public
  int iter() {
    return BitsUtil.nextSetBit(items, 0);
  }

  @Override
  public
  boolean iterValid(int iter) {
    return iter >= 0;
  }

  @Override
  public
  int iterAdvance(int iter) {
    return BitsUtil.nextSetBit(items, iter + 1);
  }

  @Override
  public
  int iterDim(int iter) {
    return iter;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof SmallDenseItemset) {
      return items == ((SmallDenseItemset) obj).items;
    }
    return super.equals(obj);
  }

  @Override
  public int compareTo(Itemset o) {
    final int l1 = length(), l2 = o.length();
    if(l1 < l2) {
      return -1;
    }
    if(l1 > l2) {
      return +1;
    }
    if(o instanceof SmallDenseItemset) {
      long oitems = ((SmallDenseItemset) o).items;
      return -Long.compare(Long.reverse(items), Long.reverse(oitems));
    }
    return super.compareLexicographical(this, o);
  }
}
