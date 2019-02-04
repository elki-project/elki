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
 * Frequent itemset.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class Itemset implements Comparable<Itemset> {
  /**
   * Support for this itemset.
   */
  int support;

  /**
   * Increase the support of the itemset.
   *
   * @return New support.
   */
  public int increaseSupport() {
    return ++support;
  }

  /**
   * Get item support.
   *
   * @return Support
   */
  public int getSupport() {
    return support;
  }

  /**
   * Test whether the itemset is contained in a bit vector.
   *
   * @param bv Bit vector
   * @return {@code true} when the itemset is contained in this vector.
   */
  public boolean containedIn(SparseNumberVector bv) {
    int i1 = this.iter(), i2 = bv.iter();
    while(this.iterValid(i1)) {
      if(!bv.iterValid(i2)) {
        return false;
      }
      int d1 = this.iterDim(i1), d2 = bv.iterDim(i2);
      if(d1 < d2) {
        return false; // Missing
      }
      if(d1 == d2) {
        if(bv.iterDoubleValue(i2) == 0.) {
          return false;
        }
        i1 = this.iterAdvance(i1);
      }
      i2 = bv.iterAdvance(i2);
    }
    return true;
  }

  /**
   * Itemset length.
   *
   * @return Itemset length
   */
  abstract public int length();

  /**
   * Get the items.
   *
   * @param i Itemset
   * @param bits Output bitset (must be zeros)
   * @return Output bitset
   */
  public static long[] toBitset(Itemset i, long[] bits) {
    for(int it = i.iter(); i.iterValid(it); it = i.iterAdvance(it)) {
      BitsUtil.setI(bits, i.iterDim(it));
    }
    return bits;
  }

  /**
   * Get an iterator over items, usually the position within an array.
   *
   * Intended usage:
   *
   * <pre>
   * {@code
   * for (int iter = v.iter(); v.iterValid(iter); iter = v.iterAdvance(iter)) {
   *   final int item = v.iterItem(iter);
   *   // Do something.
   * }
   * }
   * </pre>
   *
   * @return Iterator
   */
  public abstract int iter();

  /**
   * Advance the iterator to the next position.
   *
   * @param iter Iterator
   * @return New iterator position
   */
  public abstract int iterAdvance(int iter);

  /**
   * Check if the iterator position is valid.
   *
   * @param iter Iterator
   * @return {@code true} if the position is valid.
   */
  public abstract boolean iterValid(int iter);

  /**
   * Item at the iterator position.
   *
   * @param iter Iterator
   * @return Current item
   */
  public abstract int iterDim(int iter);

  @Override
  public int compareTo(Itemset o) {
    // Compare by length, then lexicographical.
    final int l1 = length(), l2 = o.length();
    return (l1 < l2) ? -1 : (l1 > l2) ? +1 : //
        compareLexicographical(this, o);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    }
    if(obj == null || !(obj instanceof Itemset)) {
      return false;
    }
    Itemset o = (Itemset) obj;
    int i1 = this.iter(), i2 = o.iter();
    while(this.iterValid(i1) && o.iterValid(i2)) {
      int v1 = this.iterDim(i1), v2 = o.iterDim(i2);
      if(v1 != v2) {
        return false;
      }
      i1 = this.iterAdvance(i1);
      i2 = o.iterAdvance(i2);
    }
    return this.iterValid(i1) == o.iterValid(i2);
  }

  /**
   * @deprecated Itemsets MUST NOT BE USED IN HASH MAPS.
   */
  @Deprecated
  @Override
  public int hashCode() {
    throw new APIViolationException("Itemsets may not be used in hash maps.");
  }

  /**
   * Robust compare using the iterators, lexicographical only!
   *
   * Note: This does NOT take length into account.
   *
   * @param o Other itemset.
   * @return Comparison result.
   */
  protected static int compareLexicographical(Itemset a, Itemset o) {
    int i1 = a.iter(), i2 = o.iter();
    while(a.iterValid(i1) && o.iterValid(i2)) {
      int v1 = a.iterDim(i1), v2 = o.iterDim(i2);
      if(v1 < v2) {
        return -1;
      }
      if(v2 < v1) {
        return +1;
      }
      i1 = a.iterAdvance(i1);
      i2 = o.iterAdvance(i2);
    }
    return a.iterValid(i1) ? 1 : o.iterValid(i2) ? -1 : 0;
  }

  @Override
  public String toString() {
    return appendTo(new StringBuilder(), null).toString();
  }

  /**
   * Append items and support to a string buffer.
   *
   * @param buf Buffer
   * @param meta Relation metadata (for labels)
   * @return String buffer for chaining.
   */
  public final StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    appendItemsTo(buf, meta);
    return buf.append(": ").append(support);
  }

  /**
   * Only append the items to a string buffer.
   *
   * @param buf Buffer
   * @param meta Relation metadata (for labels)
   * @return String buffer for chaining.
   */
  public StringBuilder appendItemsTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    int it = this.iter();
    if(this.iterValid(it)) {
      while(true) {
        int v = this.iterDim(it);
        String lbl = (meta != null) ? meta.getLabel(v) : null;
        if(lbl == null) {
          buf.append(v);
        }
        else {
          buf.append(lbl);
        }
        it = this.iterAdvance(it);
        if(!this.iterValid(it)) {
          break;
        }
        buf.append(", ");
      }
    }
    return buf;
  }
}
