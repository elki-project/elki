package de.lmu.ifi.dbs.elki.database.query.knn;

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
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * The interface of an actual instance.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DistanceResultPair oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface KNNQuery<O, D extends Distance<D>> extends DatabaseQuery {
  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param id query object ID
   * @param k Number of neighbors requested
   * @return neighbors
   */
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k);

  /**
   * Bulk query method
   * 
   * @param ids query object IDs
   * @param k Number of neighbors requested
   * @return neighbors
   */
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k);

  /**
   * Bulk query method configured by a map.
   * 
   * Warning: this API is not optimal, and might be removed soon (in fact, it is
   * used in a single place)
   * 
   * @param heaps Map of heaps to fill.
   */
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps);

  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param obj Query object
   * @param k Number of neighbors requested
   * @return neighbors
   */
  // TODO: return KNNList<D> instead?
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k);

  /**
   * Get the distance query for this function.
   */
  // TODO: remove?
  public DistanceQuery<O, D> getDistanceQuery();

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