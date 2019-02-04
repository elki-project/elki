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
import de.lmu.ifi.dbs.elki.utilities.exceptions.APIViolationException;

/**
 * Frequent itemset with one element.
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

  @SuppressWarnings("deprecation")
  @Override
  public boolean containedIn(SparseNumberVector bv) {
    // Ignore deprecated, as we want binary search here.
    return bv.doubleValue(item) != 0.;
  }

  @Override
  public int iter() {
    return 0;
  }

  @Override
  public boolean iterValid(int iter) {
    return iter == 0;
  }

  @Override
  public int iterAdvance(int iter) {
    return 1;
  }

  @Override
  public int iterDim(int iter) {
    assert (iter == 0);
    return item;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof OneItemset && item == ((OneItemset) obj).item) || super.equals(obj);
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
    if(o instanceof OneItemset) {
      int oitem = ((OneItemset) o).item;
      return item < oitem ? -1 : item > oitem ? +1 : 0;
    }
    return super.compareTo(o);
  }

  @Override
  public StringBuilder appendItemsTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    String lbl = (meta != null) ? meta.getLabel(item) : null;
    if(lbl == null) {
      buf.append(item);
    }
    else {
      buf.append(lbl);
    }
    return buf;
  }
}
