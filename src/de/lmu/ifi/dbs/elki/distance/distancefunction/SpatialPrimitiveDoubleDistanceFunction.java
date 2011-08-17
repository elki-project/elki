package de.lmu.ifi.dbs.elki.distance.distancefunction;
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Interface combining spatial primitive distance functions with primitive
 * number distance functions. This allows for optimization in the most common
 * types, while not sacrificing generality to support the others.
 * 
 * In essence, you should use this interface only in specialized optimized
 * codepaths.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public interface SpatialPrimitiveDoubleDistanceFunction<V extends SpatialComparable> extends SpatialPrimitiveDistanceFunction<V, DoubleDistance>, PrimitiveDoubleDistanceFunction<V> {
  /**
   * Computes the distance between the two given MBRs according to this
   * distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this
   *         distance function
   */
  double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2);

  /**
   * Computes the distance between the centroids of the two given MBRs
   * according to this distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs
   *         according to this distance function
   */
  double doubleCenterDistance(SpatialComparable mbr1, SpatialComparable mbr2);
}