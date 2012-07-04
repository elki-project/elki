package de.lmu.ifi.dbs.elki.database.ids;

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

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Pair containing a distance an an object ID
 * 
 * Note: there is no getter for the object, as this is a {@link DBIDRef}.
 * 
 * @author Erich Schubert
 *
 * @param <D> Distance
 */
public interface DistanceDBIDPair<D extends Distance<D>> extends DBIDRef {
  /**
   * Get the distance.
   * 
   * @return Distance
   */
  public D getDistance();
  
  /**
   * Compare to another result, by distance, smaller first.
   * 
   * @param other Other result
   * @return Comparison result
   */
  public int compareByDistance(DistanceDBIDPair<D> other);
}
