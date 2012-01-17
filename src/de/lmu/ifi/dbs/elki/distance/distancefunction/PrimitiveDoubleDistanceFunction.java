package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Interface for distance functions that can provide a raw double value.
 * 
 * This is for use in performance-critical situations that need to avoid the
 * boxing/unboxing cost of regular distance API.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface PrimitiveDoubleDistanceFunction<O> extends PrimitiveDistanceFunction<O, DoubleDistance> {
  /**
   * Computes the distance between two given Objects according to this distance
   * function.
   * 
   * @param o1 first Object
   * @param o2 second Object
   * @return the distance between two given Objects according to this distance
   *         function
   */
  double doubleDistance(O o1, O o2);
}