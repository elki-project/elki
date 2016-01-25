package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * APRIORI itemset.
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
   * Constructor.
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
  public boolean containedIn(BitVector bv) {
    for(int item : indices) {
      if(!bv.booleanValue(item)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public long[] getItems() {
    long[] bits = BitsUtil.zero(indices[indices.length - 1]);
    for(int item : indices) {
      BitsUtil.setI(bits, item);
    }
    return bits;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(indices);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof Itemset) || ((Itemset) obj).length() != 1) {
      return false;
    }
    // TODO: allow comparing to DenseItemset etc?
    if(getClass() != obj.getClass()) {
      return false;
    }
    return Arrays.equals(indices, ((SparseItemset) obj).indices);
  }

  @Override
  public int compareTo(Itemset o) {
    int cmp = Integer.compare(indices.length, o.length());
    if(cmp != 0) {
      return cmp;
    }
    SparseItemset other = (SparseItemset) o;
    for(int i = 0; i < indices.length; i++) {
      int c = Integer.compare(indices[i], other.indices[i]);
      if(c != 0) {
        return c;
      }
    }
    return 0;
  }

  @Override
  public StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    for(int j = 0; j < indices.length; j++) {
      if(j > 0) {
        buf.append(", ");
      }
      String lbl = (meta != null) ? meta.getLabel(indices[j]) : null;
      if(lbl == null) {
        buf.append(indices[j]);
      }
      else {
        buf.append(lbl);
      }
    }
    buf.append(": ").append(support);
    return buf;
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