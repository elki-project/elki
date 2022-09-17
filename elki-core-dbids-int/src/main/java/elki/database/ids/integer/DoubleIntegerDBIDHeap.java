/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.ids.integer;

import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDHeap;
import elki.database.ids.DoubleDBIDIter;
import elki.utilities.datastructures.heap.DoubleIntegerHeap;

/**
 * Wrapper around a primitive heap to handle DBIDs.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @composed - - - DoubleIntegerHeap
 */
class DoubleIntegerDBIDHeap implements DoubleDBIDHeap {
  /**
   * The main heap.
   */
  private final DoubleIntegerHeap heap;

  /**
   * Constructor.
   *
   * @param heap Heap to use
   */
  protected DoubleIntegerDBIDHeap(DoubleIntegerHeap heap) {
    super();
    this.heap = heap;
  }

  @Override
  public double insert(final double distance, final DBIDRef id) {
    heap.add(distance, id.internalGetIndex());
    return heap.peekKey();
  }

  @Override
  public double insert(double distance, DBIDRef id, int max) {
    heap.add(distance, id.internalGetIndex(), max);
    return heap.peekKey();
  }

  /**
   * Replace the top element.
   *
   * @param distance New distance
   * @param id New element
   */
  public void replaceTopElement(double distance, DBIDRef id) {
    heap.replaceTopElement(distance, id.internalGetIndex());
  }

  @Override
  public void poll() {
    heap.poll();
  }

  @Override
  public int size() {
    return heap.size();
  }

  @Override
  public boolean isEmpty() {
    return heap.isEmpty();
  }

  @Override
  public void clear() {
    heap.clear();
  }

  @Override
  public double peekKey() {
    return heap.isEmpty() ? Double.NaN : heap.peekKey();
  }

  @Override
  public int internalGetIndex() {
    return heap.isEmpty() ? Integer.MIN_VALUE : heap.peekValue();
  }

  @Override
  public boolean contains(DBIDRef o) {
    return heap.containsValue(o.internalGetIndex());
  }

  @Override
  public DoubleDBIDIter unorderedIterator() {
    return new UnorderedIter();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(size() * 20 + 20).append("DoubleDBIDHeap[");
    buf.append(heap.getClass());
    DoubleDBIDIter iter = this.unorderedIterator();
    if(iter.valid()) {
      buf.append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
    }
    while(iter.advance().valid()) {
      buf.append(',').append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
    }
    return buf.append(']').toString();
  }

  /**
   * Iterate over all objects in the heap, not ordered.
   *
   * @author Erich Schubert
   */
  protected class UnorderedIter implements DoubleDBIDIter {
    /**
     * Iterator of the real heap.
     */
    private DoubleIntegerHeap.UnsortedIter it = heap.unsortedIter();

    @Override
    public int internalGetIndex() {
      return it.getValue();
    }

    @Override
    public boolean valid() {
      return it.valid();
    }

    @Override
    public double doubleValue() {
      return it.getKey();
    }

    @Override
    public DoubleDBIDIter advance() {
      it.advance();
      return this;
    }
  }
}
