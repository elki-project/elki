package de.lmu.ifi.dbs.elki.database.ids.distance;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Interface for kNN heaps storing double distances and DBIDs.
 * 
 * @author Erich Schubert
 */
public interface DoubleDistanceKNNHeap extends KNNHeap<DoubleDistance> {
  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   * @return updated k-distance
   */
  double insert(double distance, DBIDRef id);

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Compared to the super.add() method, this often saves the pair construction.
   * 
   * @param distance Distance value
   * @param id ID number
   */
  @Deprecated
  void insert(Double distance, DBIDRef id);

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * 
   * Use for existing pairs.
   * 
   * @param e Existing distance pair
   */
  void insert(DoubleDistanceDBIDPair e);

  /**
   * {@inheritDoc}
   * 
   * @deprecated if you know your distances are double-valued, you should be
   *             using the primitive type.
   */
  @Override
  @Deprecated
  void insert(DoubleDistance dist, DBIDRef id);

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  double doubleKNNDistance();

  /**
   * {@inheritDoc}
   * 
   * @deprecated if you know your distances are double-valued, you should be
   *             using the primitive type.
   */
  @Override
  @Deprecated
  DoubleDistance getKNNDistance();
  
  @Override
  DoubleDistanceDBIDPair poll();
  
  @Override
  DoubleDistanceDBIDPair peek();

  @Override
  DoubleDistanceKNNList toKNNList();
}
