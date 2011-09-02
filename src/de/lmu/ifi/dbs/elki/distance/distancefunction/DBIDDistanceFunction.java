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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Distance functions valid in a database context only (i.e. for DBIDs)
 * 
 * For any "distance" that cannot be computed for arbitrary objects, only those
 * that exist in the database and referenced by their ID.
 * 
 * Example: external precomputed distances
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DBID oneway - - defined on
 * 
 * @param <D> Distance type
 */
public interface DBIDDistanceFunction<D extends Distance<?>> extends DistanceFunction<DBID, D> {
  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  D distance(DBID id1, DBID id2);
}