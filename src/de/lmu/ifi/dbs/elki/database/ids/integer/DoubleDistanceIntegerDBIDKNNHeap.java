package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleIntegerMaxHeap;

/**
 * Class to efficiently manage a kNN heap.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DoubleDistanceIntegerDBIDKNNList
 * @apiviz.composedOf DoubleIntegerMaxHeap
 */
public class DoubleDistanceIntegerDBIDKNNHeap implements DoubleDistanceKNNHeap {
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
  public DoubleDistanceIntegerDBIDKNNHeap(int k) {
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
  @Deprecated
  public DoubleDistance getKNNDistance() {
    if(heap.size() < k) {
      return DoubleDistance.INFINITE_DISTANCE;
    }
    return new DoubleDistance(kdist);
  }

  @Override
  public double doubleKNNDistance() {
    return kdist;
  }

  @Override
  @Deprecated
  public void insert(DoubleDistance distance, DBIDRef id) {
    final double dist = distance.doubleValue();
    final int iid = id.internalGetIndex();
    if(heap.size() < k) {
      heap.add(dist, iid);
      if(heap.size() >= k) {
        kdist = heap.peekKey();
      }
      return;
    }
    // Tied with top:
    if(dist >= kdist) {
      if(dist == kdist) {
        addToTies(iid);
      }
      return;
    }
    // Old top element: (kdist, previd)
    updateHeap(dist, iid);
  }

  @Override
  @Deprecated
  public void insert(Double distance, DBIDRef id) {
    final double dist = distance.doubleValue();
    final int iid = id.internalGetIndex();
    if(heap.size() < k) {
      heap.add(dist, iid);
      if(heap.size() >= k) {
        kdist = heap.peekKey();
      }
      return;
    }
    // Tied with top:
    if(dist >= kdist) {
      if(dist == kdist) {
        addToTies(iid);
      }
      return;
    }
    // Old top element: (kdist, previd)
    updateHeap(dist, iid);
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
  public void insert(final DoubleDistanceDBIDPair e) {
    final double distance = e.doubleDistance();
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
  public DoubleDistanceIntegerDBIDPair poll() {
    final DoubleDistanceIntegerDBIDPair ret;
    if(numties > 0) {
      ret = new DoubleDistanceIntegerDBIDPair(kdist, ties[numties - 1]);
      --numties;
    }
    else {
      ret = new DoubleDistanceIntegerDBIDPair(heap.peekKey(), heap.peekValue());
      heap.poll();
    }
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
  public DoubleDistanceIntegerDBIDPair peek() {
    if(numties > 0) {
      return new DoubleDistanceIntegerDBIDPair(kdist, ties[numties - 1]);
    }
    return new DoubleDistanceIntegerDBIDPair(heap.peekKey(), heap.peekValue());
  }

  @Override
  public int size() {
    return heap.size() + numties;
  }

  @Override
  public boolean isEmpty() {
    return heap.size() == 0;
  }

  @Override
  public void clear() {
    heap.clear();
    numties = 0;
  }

  @Override
  public DoubleDistanceIntegerDBIDKNNList toKNNList() {
    final int hsize = heap.size();
    DoubleDistanceIntegerDBIDKNNList ret = new DoubleDistanceIntegerDBIDKNNList(k, hsize + numties);
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
    ret.sort();
    return ret;
  }

  /**
   * Peek the topmost distance.
   * 
   * @return distance
   */
  protected double peekDistance() {
    if(numties > 0) {
      return kdist;
    }
    else {
      return heap.peekKey();
    }
  }

  /**
   * Peek the topmost internal ID.
   * 
   * @return internal id
   */
  protected int peekInternalDBID() {
    if(numties > 0) {
      return ties[numties - 1];
    }
    return heap.peekValue();
  }
}
