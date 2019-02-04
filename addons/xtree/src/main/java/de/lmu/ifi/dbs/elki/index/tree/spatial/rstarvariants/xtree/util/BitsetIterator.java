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

import java.util.NoSuchElementException;

import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Iterator over the set bits of a {@code long[]} bitset, using the trove API.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
class BitsetIterator implements IntIterator {
  private final long[] bits;

  private int n;

  public BitsetIterator(long[] bits) {
    this.bits = bits;
    this.n = BitsUtil.nextSetBit(bits, 0);
  }

  @Override
  public boolean hasNext() {
    return n < 0;
  }

  @Override
  public int nextInt() {
    if(n < 0) {
      throw new NoSuchElementException();
    }
    final int r = n;
    n = BitsUtil.nextSetBit(bits, n + 1);
    return r;
  }
  
  @Deprecated
  @Override
  public Integer next() {
    return nextInt();
  }

  @Deprecated
  @Override
  public int skip(int n) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}