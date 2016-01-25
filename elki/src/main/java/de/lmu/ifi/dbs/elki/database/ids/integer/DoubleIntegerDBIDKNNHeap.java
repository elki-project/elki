package de.lmu.ifi.dbs.elki.database.ids.integer;

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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleIntegerMaxHeap;

/**
 * Class to efficiently manage a kNN heap.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has DoubleIntegerDBIDKNNList
 * @apiviz.composedOf DoubleIntegerMaxHeap
 */
class DoubleIntegerDBIDKNNHeap implements KNNHeap {
  /**
   * k for this heap.
   */
  private final int k;

  /**
   * The main heap.
   */
  private final DoubleIntegerMaxHeap heap;

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
    super();
    this.k = k;
    this.heap = new DoubleIntegerMaxHeap(k);
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
    final int iid = id.internalGetIndex();
    if(heap.size() < k) {
      heap.add(distance, iid);
      if(heap.size() >= k) {
        kdist = heap.peekKey();
      }
      return kdist;
    }
    // Tied with top:
    if(distance >= kdist) {
      if(distance == kdist) {
        addToTies(iid);
      }
      return kdist;
    }
    // Old top element: (kdist, previd)
    updateHeap(distance, iid);
    return kdist;
  }

  @Override
  public void insert(final DoubleDBIDPair e) {
    final double distance = e.doubleValue();
    final int iid = e.internalGetIndex();
    if(heap.size() < k) {
      heap.add(distance, iid);
      if(heap.size() >= k) {
        kdist = heap.peekKey();
      }
      return;
    }
    // Tied with top:
    if(distance >= kdist) {
      if(distance == kdist) {
        addToTies(iid);
      }
      return;
    }
    // Old top element: (kdist, previd)
    updateHeap(distance, iid);
  }

  /**
   * Do a full update for the heap.
   * 
   * @param distance Distance
   * @param iid Object id
   */
  private final void updateHeap(final double distance, final int iid) {
    final double prevdist = kdist;
    final int previd = heap.peekValue();
    heap.replaceTopElement(distance, iid);
    kdist = heap.peekKey();
    // If the kdist improved, zap ties.
    if(kdist < prevdist) {
      numties = 0;
    }
    else {
      addToTies(previd);
    }
  }

  /**
   * Ensure the ties array has capacity for at least one more element.
   * 
   * @param id Id to add
   */
  private final void addToTies(int id) {
    if(ties.length == numties) {
      ties = Arrays.copyOf(ties, (ties.length << 1) + 1); // grow.
    }
    ties[numties] = id;
    ++numties;
  }

  @Override
  public DoubleIntegerDBIDPair poll() {
    if(numties > 0) {
      return new DoubleIntegerDBIDPair(kdist, ties[--numties]);
    }
    final DoubleIntegerDBIDPair ret = new DoubleIntegerDBIDPair(heap.peekKey(), heap.peekValue());
    heap.poll();
    return ret;
  }

  /**
   * Pop the topmost element.
   */
  protected void pop() {
    if(numties > 0) {
      --numties;
    }
    else {
      heap.poll();
    }
  }

  @Override
  public DoubleIntegerDBIDPair peek() {
    if(numties > 0) {
      return new DoubleIntegerDBIDPair(kdist, ties[numties - 1]);
    }
    return new DoubleIntegerDBIDPair(heap.peekKey(), heap.peekValue());
  }

  @Override
  public int size() {
    return heap.size() + numties;
  }

  @Override
  public boolean isEmpty() {
    return heap.isEmpty();
  }

  @Override
  public void clear() {
    heap.clear();
    numties = 0;
  }

  @Override
  public DoubleIntegerDBIDKNNList toKNNList() {
    final int hsize = heap.size();
    DoubleIntegerDBIDKNNList ret = new DoubleIntegerDBIDKNNList(k, hsize + numties);
    // Add ties:
    for(int i = 0; i < numties; i++) {
      ret.dists[hsize + i] = kdist;
      ret.ids[hsize + i] = ties[i];
    }
    for(int j = hsize - 1; j >= 0; j--) {
      ret.dists[j] = heap.peekKey();
      ret.ids[j] = heap.peekValue();
      heap.poll();
    }
    ret.size = hsize + numties;
    return ret;
  }

  /**
   * Peek the topmost distance.
   * 
   * @return distance
   */
  protected double peekDistance() {
    return (numties > 0) ? kdist : heap.peekKey();
  }

  /**
   * Peek the topmost internal ID.
   * 
   * @return internal id
   */
  protected int peekInternalDBID() {
    return (numties > 0) ? ties[numties - 1] : heap.peekValue();
  }
}
