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

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Frequent itemset, sparse representation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SparseItemset extends Itemset {
  /**
   * Items, as indexes.
   */
  int[] indices;

  /**
   * Constructor.
   *
   * @param indices Items
   */
  public SparseItemset(int[] indices) {
    this.indices = indices;
  }

  /**
   * Constructor with initial support.
   *
   * @param indices Items
   * @param support Support
   */
  public SparseItemset(int[] indices, int support) {
    this.indices = indices;
    this.support = support;
  }

  /**
   * Constructor from existing itemsets.
   * 
   * @param ii First 1-itemset
   * @param ij Second 1-itemset
   */
  public SparseItemset(OneItemset ii, OneItemset ij) {
    if(ii.item == ij.item) {
      throw new AbortException("SparseItemset constructed from identical 1-itemsets.");
    }
    this.indices = (ii.item < ij.item) ? new int[] { ii.item, ij.item } : new int[] { ij.item, ii.item };
  }

  @Override
  public int length() {
    return indices.length;
  }

  @Override
  public
  int iter() {
    return 0;
  }

  @Override
  public
  boolean iterValid(int iter) {
    return iter < indices.length;
  }

  @Override
  public
  int iterAdvance(int iter) {
    return ++iter;
  }

  @Override
  public
  int iterDim(int iter) {
    return indices[iter];
  }

  @Override
  public int compareTo(Itemset o) {
    // Compare by length, then lexicographical.
    final int l1 = length(), l2 = o.length();
    if(l1 < l2) {
      return -1;
    }
    if(l1 > l2) {
      return +1;
    }
    if(o instanceof SparseItemset) {
      SparseItemset other = (SparseItemset) o;
      for(int i = 0; i < indices.length; i++) {
        int v1 = indices[i], v2 = other.indices[i];
        if(v1 < v2) {
          return -1;
        }
        if(v1 > v2) {
          return +1;
        }
      }
      return 0;
    }
    return super.compareLexicographical(this, o);
  }

  /**
   * Perform the prefix test for sparse itemset.
   * 
   * @param other Other itemset
   * @return {@code true} iff the first n-1 items agree.
   */
  public boolean prefixTest(SparseItemset other) {
    if(indices.length != other.indices.length) {
      throw new AbortException("PrefixTest is only valid for itemsets of the same length!");
    }
    for(int k = indices.length - 2; k >= 0; k--) {
      if(indices[k] != other.indices[k]) {
        return false;
      }
    }
    return true;
  }
}
