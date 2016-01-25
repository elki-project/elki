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

/**
 * APRIORI itemset.
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
  abstract public boolean containedIn(BitVector bv);

  /**
   * Itemset length.
   * 
   * @return Itemset length
   */
  abstract public int length();

  /**
   * Get the items.
   * 
   * @return Itemset contents.
   */
  abstract public long[] getItems();

  @Override
  public String toString() {
    return appendTo(new StringBuilder(), null).toString();
  }

  /**
   * Append to a string buffer.
   * 
   * @param buf Buffer
   * @param meta Relation metadata (for labels)
   * @return String buffer for chaining.
   */
  abstract public StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta);
}