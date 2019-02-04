/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;

/**
 * Query interface for spatial distance queries.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <V> Vector type
 */
public interface SpatialDistanceQuery<V extends SpatialComparable> extends DistanceQuery<V> {
  /**
   * Computes the minimum distance between the given MBR and the FeatureVector
   * object according to this distance function.
   * 
   * @param mbr the MBR object
   * @param v the FeatureVector object
   * @return the minimum distance between the given MBR and the FeatureVector
   *         object according to this distance function
   */
  double minDist(SpatialComparable mbr, V v);

  /**
   * Computes the minimum distance between the given MBR and the FeatureVector
   * object according to this distance function.
   * 
   * @param mbr the MBR object
   * @param id the query object id
   * @return the minimum distance between the given MBR and the FeatureVector
   *         object according to this distance function
   */
  double minDist(SpatialComparable mbr, DBIDRef id);

  /**
   * Get the inner distance function.
   * 
   * @return Distance function
   */
  @Override
  SpatialPrimitiveDistanceFunction<? super V> getDistanceFunction();
}