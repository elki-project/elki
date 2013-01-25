package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
    if (heap.size() < k) {
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
  public void add(DoubleDistance distance, DBIDRef id) {
    add(distance.doubleValue(), id);
  }

  @Override
  @Deprecated
  public void add(Double distance, DBIDRef id) {
    add(distance.doubleValue(), id);
  }

  @Override
  public final void add(final double distance, final DBIDRef id) {
    if (distance > kdist) {
      return;
    }
    final int iid = id.internalGetIndex();
    if (heap.size() < k) {
      heap.add(distance, iid);
      if (heap.size() >= k) {
        kdist = heap.peekKey();
      }
      return;
    }
    // Tied with top:
    if (distance >= kdist) {
      addToTies(iid);
      return;
    }
    // Old top element: (kdist, previd)
    updateHeap(distance, iid);
  }

  @Override
  public void add(DoubleDistanceDBIDPair e) {
    add(e.doubleDistance(), e);
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
    if (kdist < prevdist) {
      numties = 0;
    } else {
      addToTies(previd);
    }
  }

  /**
   * Ensure the ties array has capacity for at least one more element.
   * 
   * @param id Id to add
   */
  private final void addToTies(int id) {
    if (ties.length == numties) {
      ties = Arrays.copyOf(ties, ties.length << 1); // grow.
    }
    ties[numties++] = id;
  }

  @Override
  public DoubleDistanceIntegerDBIDPair poll() {
    final int last = numties - 1;
    final DoubleDistanceIntegerDBIDPair ret;
    if (last >= 0) {
      ret = new DoubleDistanceIntegerDBIDPair(kdist, ties[last]);
      --numties;
    } else {
      ret = new DoubleDistanceIntegerDBIDPair(heap.peekKey(), heap.peekValue());
      heap.poll();
    }
    return ret;
  }

  /**
   * Pop the topmost element.
   */
  protected void pop() {
    if (numties > 0) {
      --numties;
    } else {
      heap.poll();
    }
  }

  @Override
  public DoubleDistanceIntegerDBIDPair peek() {
    final int last = numties - 1;
    if (last >= 0) {
      return new DoubleDistanceIntegerDBIDPair(kdist, ties[last]);
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
    // ties = null;
  }

  @Override
  public DoubleDistanceIntegerDBIDKNNList toKNNList() {
    return new DoubleDistanceIntegerDBIDKNNList(this);
  }

  /**
   * Peek the topmost distance.
   * 
   * @return distance
   */
  protected double peekDistance() {
    if (numties > 0) {
      return kdist;
    } else {
      return heap.peekKey();
    }
  }

  /**
   * Peek the topmost internal ID.
   * 
   * @return internal id
   */
  protected int peekInternalDBID() {
    if (numties > 0) {
      return ties[numties - 1];
    }
    return heap.peekValue();
  }
}
