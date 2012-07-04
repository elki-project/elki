package de.lmu.ifi.dbs.elki.database.query;

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

import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Iterator for double valued distance-based query results.
 * 
 * @author Erich Schubert
 */
public interface DoubleDistanceDBIDResultIter extends DistanceDBIDResultIter<DoubleDistance> {
  /**
   * Get the distance
   * 
   * @return distance
   */
  public double doubleDistance();
  
  /**
   * Get an object pair.
   * 
   * @return object pair
   */
  @Override
  public DoubleDistanceDBIDPair getDistancePair();
  
  /**
   * Get the distance
   * 
   * @deprecated Use {@link doubleDistance} to avoid creating unneccessary
   *             objects.
   * 
   * @return distance
   */
  @Deprecated
  @Override
  public DoubleDistance getDistance();
}