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

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Heap for collecting kNN candidates with arbitrary distance types.
 * 
 * For double distances, see {@link DoubleDistanceDBIDPairKNNHeap}
 * 
 * <b>To instantiate, use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newHeap} instead!</b>
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class DistanceDBIDPairKNNHeap<D extends Distance<D>> extends AbstractKNNHeap<DistanceDBIDPair<D>, D> {
  /**
   * Cached distance to k nearest neighbor (to avoid going through {@link #peek}
   * each time).
   */
  protected D knndistance = null;

  /**
   * Constructor.
   * 
   * <b>To instantiate, use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newHeap} instead!</b>
   * 
   * @param k Heap size
   */
  public DistanceDBIDPairKNNHeap(int k) {
    super(k);
  }

  /**
   * Serialize to a {@link DistanceDBIDPairKNNList}. This empties the heap!
   * 
   * @return KNNList with the heaps contents.
   */
  @Override
  public DistanceDBIDPairKNNList<D> toKNNList() {
    return new DistanceDBIDPairKNNList<>(this);
  }

  @Override
  public void insert(D distance, DBIDRef id) {
    if (size() < getK()) {
      heap.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
      heapModified();
      return;
    }
    // size >= maxsize. Insert only when necessary.
    if (knndistance.compareTo(distance) >= 0) {
      // Replace worst element.
      heap.add(DBIDFactory.FACTORY.newDistancePair(distance, id));
      heapModified();
    }
  }

  @Override
  public void insert(DistanceDBIDPair<D> pair) {
    if (size() < getK() || knndistance.compareTo(pair.getDistance()) >= 0) {
      heap.add(pair);
      heapModified();
    }
  }

  // @Override
  protected void heapModified() {
    // super.heapModified();
    // Update threshold.
    if (size() >= getK()) {
      knndistance = heap.peek().getDistance();
    }
  }

  @Override
  public D getKNNDistance() {
    return knndistance;
  }
}
