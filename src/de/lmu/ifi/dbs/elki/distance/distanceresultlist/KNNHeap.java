package de.lmu.ifi.dbs.elki.distance.distanceresultlist;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface for kNN heaps.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.uses KNNResult - - «serializes to»
 * @apiviz.composedOf DistanceDBIDPair
 * 
 * @param <D> Distance function
 */
public interface KNNHeap<D extends Distance<D>> {
  /**
   * Serialize to a {@link GenericKNNList}. This empties the heap!
   * 
   * @return KNNList with the heaps contents.
   */
  public KNNResult<D> toKNNList();

  /**
   * Get the K parameter ("maxsize" internally).
   * 
   * @return K
   */
  public int getK();

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  public D getKNNDistance();

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   */
  public void add(D distance, DBIDRef id);

  /**
   * Current size of heap.
   * 
   * @return Heap size
   */
  public int size();

  /**
   * Poll the <em>largest</em> element from the heap.
   * 
   * This is in descending order because of the heap structure. For a convenient
   * way to serialize the heap into a list that you can iterate in ascending
   * order, see {@link #toKNNList()}.
   * 
   * @return largest element
   */
  public DistanceDBIDPair<D> poll();
}