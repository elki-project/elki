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
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
/**
 * Modifiable API for Distance-DBID results
 * 
 * @author Erich Schubert
 *
 * @param <D> Distance type
 */
public interface ModifiableDistanceDBIDResult<D extends Distance<D>> extends DistanceDBIDResult<D> {
  /**
   * Add an object to this result.
   * 
   * @param distance Distance to add
   * @param id DBID to add
   */
  public void add(D distance, DBIDRef id);

  /**
   * Sort the result in ascending order
   */
  public void sort();
}