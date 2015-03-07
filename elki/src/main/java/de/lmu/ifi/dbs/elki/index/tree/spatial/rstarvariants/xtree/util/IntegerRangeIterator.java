package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.util;

import gnu.trove.iterator.TIntIterator;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

/**
 * Iterator provider for an integer range from <code>from</code> to
 * <code>to</code>. Covers <code>[from,to[</code>.
 */
public class IntegerRangeIterator implements TIntIterator {
  protected int n, to;

  public IntegerRangeIterator(int from, int to) {
    this.n = from;
    this.to = to;
  }

  @Override
  public boolean hasNext() {
    return n < to;
  }

  @Override
  public int next() {
    return n++; // Post-increment!
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}