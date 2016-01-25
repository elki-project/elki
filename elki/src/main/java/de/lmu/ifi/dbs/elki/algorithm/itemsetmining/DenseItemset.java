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

/**
 * APRIORI itemset.
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
  public boolean containedIn(BitVector bv) {
    return bv.contains(items);
  }

  @Override
  public long[] getItems() {
    return items;
  }

  @Override
  public int hashCode() {
    return BitsUtil.hashCode(items);
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
    // TODO: allow comparison to DenseItemset?
    if(getClass() != obj.getClass()) {
      return false;
    }
    return BitsUtil.equal(items, ((DenseItemset) obj).items);
  }

  @Override
  public int compareTo(Itemset o) {
    int cmp = Integer.compare(length, o.length());
    if(cmp != 0) {
      return cmp;
    }
    DenseItemset other = (DenseItemset) o;
    for(int i = 0; i < items.length; i++) {
      if(items[i] != other.items[i]) {
        return -Long.compare(Long.reverse(items[i]), Long.reverse(other.items[i]));
      }
    }
    return 0;
  }

  @Override
  public StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
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
    buf.append(": ").append(support);
    return buf;
  }
}