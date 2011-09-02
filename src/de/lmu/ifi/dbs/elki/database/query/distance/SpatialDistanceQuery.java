package de.lmu.ifi.dbs.elki.database.query.distance;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Query interface for spatial distance queries.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
public interface SpatialDistanceQuery<V extends SpatialComparable, D extends Distance<D>> extends DistanceQuery<V, D> {
  /**
   * Computes the minimum distance between the given MBR and the FeatureVector
   * object according to this distance function.
   * 
   * @param mbr the MBR object
   * @param v the FeatureVector object
   * @return the minimum distance between the given MBR and the FeatureVector
   *         object according to this distance function
   */
  D minDist(SpatialComparable mbr, V v);

  /**
   * Computes the minimum distance between the given MBR and the FeatureVector
   * object according to this distance function.
   * 
   * @param mbr the MBR object
   * @param id the query object id
   * @return the minimum distance between the given MBR and the FeatureVector
   *         object according to this distance function
   */
  D minDist(SpatialComparable mbr, DBID id);

  /**
   * Computes the distance between the two given MBRs according to this distance
   * function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this distance
   *         function
   */
  D mbrDist(SpatialComparable mbr1, SpatialComparable mbr2);

  /**
   * Computes the distance between the centroids of the two given MBRs according
   * to this distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs according
   *         to this distance function
   */
  D centerDistance(SpatialComparable mbr1, SpatialComparable mbr2);

  /**
   * Get the inner distance function.
   * 
   * @return Distance function
   */
  @Override
  SpatialPrimitiveDistanceFunction<? super V, D> getDistanceFunction();
}