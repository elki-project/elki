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
public class OneItemset extends Itemset {
  /**
   * Trivial item.
   */
  int item;

  /**
   * Constructor of 1-itemset.
   * 
   * @param item Item
   */
  public OneItemset(int item) {
    this.item = item;
  }

  /**
   * Constructor with initial support.
   * 
   * @param item Item
   * @param support Support
   */
  public OneItemset(int item, int support) {
    this.item = item;
    this.support = support;
  }

  @Override
  public int length() {
    return 1;
  }

  @Override
  public boolean containedIn(BitVector bv) {
    // TODO: add a booleanValue method to BitVector?
    return bv.longValue(item) != 0L;
  }

  @Override
  public long[] getItems() {
    long[] bits = BitsUtil.zero(item);
    BitsUtil.setI(bits, item);
    return bits;
  }

  @Override
  public int hashCode() {
    return item;
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
    if(getClass() != obj.getClass()) {
      return false;
    }
    OneItemset other = (OneItemset) obj;
    return item == other.item;
  }

  @Override
  public int compareTo(Itemset o) {
    int cmp = Integer.compare(1, o.length());
    if(cmp != 0) {
      return cmp;
    }
    if(o instanceof OneItemset) {
      return Integer.compare(item, ((OneItemset) o).item);
    }
    throw new AbortException("Itemset of length 1 not using OneItemset!");
  }

  @Override
  public StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    String lbl = (meta != null) ? meta.getLabel(item) : null;
    if(lbl == null) {
      buf.append(item);
    }
    else {
      buf.append(lbl);
    }
    return buf.append(": ").append(support);
  }
}