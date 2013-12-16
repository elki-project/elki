package de.lmu.ifi.dbs.elki.database.ids.generic;

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


import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TiedTopBoundedHeap;

/**
 * Heap used for KNN management.
 * 
 * @author Erich Schubert
 * 
 * @param <P> pair type
 * @param <D> distance type
 */
abstract class AbstractKNNHeap<P extends DistanceDBIDPair<D>, D extends Distance<D>> implements KNNHeap<D> {
  /**
   * The actual heap.
   */
  protected final TiedTopBoundedHeap<P> heap;

  /**
   * Constructor.
   * 
   * @param k Maximum heap size (unless tied)
   */
  public AbstractKNNHeap(int k) {
    super();
    heap = new TiedTopBoundedHeap<>(k, DistanceDBIDResultUtil.BY_REVERSE_DISTANCE);
  }

  /**
   * Add a pair to the heap.
   * 
   * @param pair Pair to add.
   */
  public abstract void insert(P pair);

  @Override
  public final int getK() {
    return heap.getMaxSize();
  }

  @Override
  public int size() {
    return heap.size();
  }
  
  @Override
  public P peek() {
    return heap.peek();
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
  public P poll() {
    return heap.poll();
  }
}