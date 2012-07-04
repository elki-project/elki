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

import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Pair containing a double distance a DBID.
 * 
 * There is no getter for the DBID, as this is a {@link DBIDRef} already.
 * 
 * @author Erich Schubert
 */
public interface DoubleDistanceDBIDPair extends DistanceDBIDPair<DoubleDistance> {
  /**
   * Get the distance.
   * 
   * @deprecated Would produce a DoubleDistance object. Use {@link #doubleDistance} instead!
   * 
   * @return Distance
   */
  @Override
  @Deprecated
  public DoubleDistance getDistance();

  /**
   * Get the distance.
   * 
   * @return Distance
   */
  public double doubleDistance();
}