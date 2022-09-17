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

import java.util.Arrays;

import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDIter;
import elki.database.ids.KNNHeap;
import elki.utilities.datastructures.heap.DoubleIntegerMaxHeap;

/**
 * Class to efficiently manage a kNN heap.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - DoubleIntegerDBIDKNNList
 * @composed - - - DoubleIntegerMaxHeap
 */
class DoubleIntegerDBIDKNNHeap extends DoubleIntegerDBIDHeap implements KNNHeap {
  /**
   * k for this heap.
   */
  private final int k;

  /**
   * List to track ties.
   */
  private int[] ties;

  /**
   * Number of element in ties list.
   */
  private int numties = 0;

  /**
   * Current maximum value.
   */
  private double kdist = Double.POSITIVE_INFINITY;

  /**
   * Initial size of ties array.
   */
  private static final int INITIAL_TIES_SIZE = 11;

  /**
   * Constructor.
   *
   * @param k Size of knn.
   */
  protected DoubleIntegerDBIDKNNHeap(int k) {
    super(new DoubleIntegerMaxHeap(k));
    this.k = k;
    this.ties = new int[INITIAL_TIES_SIZE];
  }

  @Override
  public int getK() {
    return k;
  }

  @Override
  public double getKNNDistance() {
    return kdist;
  }

  @Override
  public final double insert(final double distance, final DBIDRef id) {
    if(super.size() < k) {
      super.insert(distance, id);
      // Update kdist if size == k!
      return (super.size() >= k) ? kdist = super.peekKey() : kdist;
    }
    // Better than top
    if(distance < kdist) {
      // Old top element: (kdist, previd)
      final double prevdist = kdist;
      assert kdist == super.peekKey();
      final int previd = super.internalGetIndex();
      super.replaceTopElement(distance, id);
      kdist = super.peekKey();
      // If the kdist improved, zap ties.
      if(kdist < prevdist) {
        numties = 0;
      }
      else {
        addToTies(previd);
      }
    }
    else if(distance == kdist) {
      addToTies(id.internalGetIndex());
    }
    return kdist;
  }

  /**
   * Ensure the ties array has capacity for at least one more element.
   *
   * @param id Id to add
   */
  private void addToTies(int id) {
    if(ties.length == numties) {
      ties = Arrays.copyOf(ties, (ties.length << 1) + 1); // grow.
    }
    ties[numties] = id;
    ++numties;
  }

  @Override
  public void poll() {
    if(numties > 0) {
      --numties;
    }
    else {
      super.poll();
    }
  }

  @Override
  public int size() {
    return super.size() + numties;
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public void clear() {
    super.clear();
    numties = 0;
    kdist = Double.POSITIVE_INFINITY;
  }

  @Override
  public DoubleIntegerDBIDKNNList toKNNList() {
    final int hsize = super.size();
    DoubleIntegerDBIDKNNList ret = new DoubleIntegerDBIDKNNList(k, hsize + numties);
    // Add ties:
    for(int i = 0; i < numties; i++) {
      ret.dists[hsize + i] = kdist;
      ret.ids[hsize + i] = ties[i];
    }
    for(int j = hsize - 1; j >= 0; j--) {
      ret.dists[j] = super.peekKey();
      ret.ids[j] = super.internalGetIndex();
      super.poll();
    }
    ret.size = hsize + numties;
    return ret;
  }

  @Override
  public DoubleIntegerDBIDKNNList toKNNListSqrt() {
    final int hsize = super.size();
    DoubleIntegerDBIDKNNList ret = new DoubleIntegerDBIDKNNList(k, hsize + numties);
    // Add ties:
    double kdist = numties > 0 ? Math.sqrt(this.kdist) : 0.;
    for(int i = 0; i < numties; i++) {
      ret.dists[hsize + i] = kdist;
      ret.ids[hsize + i] = ties[i];
    }
    for(int j = hsize - 1; j >= 0; j--) {
      ret.dists[j] = Math.sqrt(super.peekKey());
      ret.ids[j] = super.internalGetIndex();
      super.poll();
    }
    ret.size = hsize + numties;
    return ret;
  }

  @Override
  public double peekKey() {
    return super.isEmpty() ? Double.NaN : super.peekKey();
  }

  @Override
  public int internalGetIndex() {
    return numties > 0 ? ties[numties - 1] : super.isEmpty() ? Integer.MIN_VALUE : super.internalGetIndex();
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int q = o.internalGetIndex();
    for(int i = 0; i < numties; i++) {
      if(ties[i] == q) {
        return true;
      }
    }
    return super.contains(o);
  }

  @Override
  public DoubleDBIDIter unorderedIterator() {
    return new UnorderedIter(super.unorderedIterator());
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(size() * 20 + 20).append("KNNHeap[");
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
  private class UnorderedIter implements DoubleDBIDIter {
    /**
     * Iterator of the real heap.
     */
    private DoubleDBIDIter it;

    /**
     * Position in ties.
     */
    private int t = 0;

    /**
     * Constructor.
     *
     * @param it Parent iterator
     */
    public UnorderedIter(DoubleDBIDIter it) {
      this.it = it;
    }

    @Override
    public int internalGetIndex() {
      return it.valid() ? it.internalGetIndex() : ties[t];
    }

    @Override
    public boolean valid() {
      return it.valid() || t < numties;
    }

    @Override
    public double doubleValue() {
      return it.valid() ? it.doubleValue() : kdist;
    }

    @Override
    public DoubleDBIDIter advance() {
      if(it.valid()) {
        it.advance();
      }
      else {
        ++t;
      }
      return this;
    }
  }
}
