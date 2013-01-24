package de.lmu.ifi.dbs.elki.database.ids.distance;

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
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * An object containing Double-DBID-Pairs.
 * 
 * @author Erich Schubert
 */
public interface ModifiableDoubleDistanceDBIDList extends DoubleDistanceDBIDList, ModifiableDistanceDBIDList<DoubleDistance> {
  /**
   * Add an element.
   * 
   * @deprecated Pass a double value instead.
   * 
   * @param dist Distance
   * @param id ID
   */
  @Override
  @Deprecated
  void add(DoubleDistance dist, DBIDRef id);

  /**
   * Add an element.
   * 
   * @param dist Distance
   * @param id ID
   */
  void add(double dist, DBIDRef id);

  /**
   * Add an element.
   * 
   * @param pair Pair to add
   */
  void add(DoubleDistanceDBIDPair pair);
}
