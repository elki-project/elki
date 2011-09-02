package de.lmu.ifi.dbs.elki.database.query.range;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * The interface for range queries
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DistanceResultPair oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface RangeQuery<O, D extends Distance<D>> extends DatabaseQuery {
  /**
   * Get the nearest neighbors for a particular id in a given query range
   * 
   * @param id query object ID
   * @param range Query range
   * @return neighbors
   */
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range);

  /**
   * Bulk query method
   * 
   * @param ids query object IDs
   * @param range Query range
   * @return neighbors
   */
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range);

  /**
   * Get the nearest neighbors for a particular object in a given query range
   * 
   * @param obj Query object
   * @param range Query range
   * @return neighbors
   */
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range);

  /**
   * Get the distance data type of the function.
   */
  public D getDistanceFactory();
  
  /**
   * Access the underlying data query.
   * 
   * @return data query in use
   */
  public abstract Relation<? extends O> getRelation();
}