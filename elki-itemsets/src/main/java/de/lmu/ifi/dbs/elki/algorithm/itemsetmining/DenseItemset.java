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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.APIViolationException;

/**
 * APRIORI itemset, dense representation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DenseItemset extends Itemset {
  /**
   * Items, as a bitmask.
   */
  long[] items;

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
  public DenseItemset(long[] items, int length) {
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
      return ((BitVector) bv).contains(items);
    }
    return super.containedIn(bv);
  }

  @Override
  public int iter() {
    return BitsUtil.nextSetBit(items, 0);
  }

  @Override
  public boolean iterValid(int iter) {
    return iter >= 0;
  }

  @Override
  public int iterAdvance(int iter) {
    return BitsUtil.nextSetBit(items, iter + 1);
  }

  @Override
  public int iterDim(int iter) {
    return iter;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof DenseItemset && BitsUtil.equal(items, ((DenseItemset) obj).items)) || super.equals(obj);
  }

  /**
   * @deprecated Itemsets MUST NOT BE USED IN HASH MAPS.
   */
  @Deprecated
  @Override
  public int hashCode() {
    throw new APIViolationException("Itemsets may not be used in hash maps.");
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
    if(o instanceof DenseItemset) {
      DenseItemset other = (DenseItemset) o;
      for(int i = 0; i < items.length; i++) {
        if(items[i] != other.items[i]) {
          return -Long.compare(Long.reverse(items[i]), Long.reverse(other.items[i]));
        }
      }
      return 0;
    }
    return super.compareLexicographical(this, o);
  }

  @Override
  public StringBuilder appendItemsTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    int i = BitsUtil.nextSetBit(items, 0);
    while(true) {
      String lbl = (meta != null) ? meta.getLabel(i) : null;
      if(lbl == null) {
        buf.append(i);
      }
      else {
        buf.append(lbl);
      }
      i = BitsUtil.nextSetBit(items, i + 1);
      if(i < 0) {
        break;
      }
      buf.append(", ");
    }
    return buf;
  }
}
