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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * History of all splits ever occurred in a Node.
 * 
 * @author Marisa Thoma
 * @author Erich Schubert
 * @since 0.7.5
 */
public final class SplitHistory implements Serializable, Cloneable {
  /**
   * Serialization version number.
   */
  private static final long serialVersionUID = -340123050472355301L;

  /**
   * Bitset with a bit for each dimension
   */
  private long[] dimBits;

  /**
   * Initialize a new split history instance of dimension <code>dim</code>.
   * 
   * @param dim
   */
  public SplitHistory(int dim) {
    dimBits = BitsUtil.zero(dim);
  }

  /**
   * Constructor.
   * 
   * @param bitset Existing bitset
   */
  private SplitHistory(long[] bitset) {
    this.dimBits = bitset;
  }

  /**
   * Set dimension <code>dimension</code> to <code>true</code>
   * 
   * @param dimension
   */
  public void setDim(int dimension) {
    BitsUtil.setI(dimBits, dimension);
  }

  /**
   * Get the common split dimensions from a list of split histories.
   * 
   * @param splitHistories
   * @return list of split dimensions
   */
  public static IntIterator getCommonDimensions(Collection<SplitHistory> splitHistories) {
    Iterator<SplitHistory> it = splitHistories.iterator();
    long[] checkSet = BitsUtil.copy(it.next().dimBits);
    while(it.hasNext()) {
      SplitHistory sh = it.next();
      BitsUtil.andI(checkSet, sh.dimBits);
    }
    return new BitsetIterator(checkSet);
  }

  @Override
  public String toString() {
    return BitsUtil.toString(dimBits);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    SplitHistory c = (SplitHistory) super.clone();
    c.dimBits = this.dimBits.clone();
    return c;
  }

  /**
   * Writes the split history to the specified output stream.
   * 
   * @param out the stream to write the history to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(dimBits);
  }

  /**
   * Reads the split history from the specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  public static SplitHistory readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    return new SplitHistory((long[]) in.readObject());
  }

  public boolean isEmpty() {
    return BitsUtil.isZero(dimBits);
  }
}
