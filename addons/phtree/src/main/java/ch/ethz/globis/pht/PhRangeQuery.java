package ch.ethz.globis.pht;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

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

import ch.ethz.globis.pht.PhTree.PhIterator;
import ch.ethz.globis.pht.PhTree.PhQuery;

/**
 * Range query.
 * 
 * @author Tilmann Zaeschke
 *
 * @param <T>
 */
public class PhRangeQuery<T> implements PhIterator<T> {

  private final long[] min, max;
  private final PhQuery<T> q;
  private final int DIM;
  private final PhDistance dist;
  private final PhFilterDistance filter;

  public PhRangeQuery(PhQuery<T> iter, PhTree<T> tree, 
      PhDistance dist, PhFilterDistance filter) {
    this.DIM = tree.getDIM();
    this.q = iter;
    this.dist = dist;
    this.filter = filter;
    this.min = new long[DIM];
    this.max = new long[DIM];
  }

  public PhRangeQuery<T> reset(double range, long... center) {
    filter.set(center, dist, range);
    dist.toMBB(range, center, min, max);
    q.reset(min, max);
    return this;
  }

  @Override
  public long[] nextKey() {
    return q.nextKey();
  }

  @Override
  public T nextValue() {
    return q.nextValue();
  }

  @Override
  public PhEntry<T> nextEntry() {
    return q.nextEntry();
  }

  @Override
  public boolean hasNext() {
    return q.hasNext();
  }

  @Override
  public T next() {
    return q.next();
  }

  @Override
  public void remove() {
    q.remove();
  }

  @Override
  public PhEntry<T> nextEntryReuse() {
    return q.nextEntryReuse();
  }

}
