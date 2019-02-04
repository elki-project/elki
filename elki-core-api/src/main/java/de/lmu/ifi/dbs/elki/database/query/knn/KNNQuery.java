/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;

/**
 * The interface of an actual instance.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @navhas - create - KNNList
 * 
 * @param <O> Object type
 */
public interface KNNQuery<O> extends DatabaseQuery {
  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param id query object ID
   * @param k Number of neighbors requested
   * @return neighbors
   */
  KNNList getKNNForDBID(DBIDRef id, int k);

  /**
   * Bulk query method
   * 
   * @param ids query object IDs
   * @param k Number of neighbors requested
   * @return neighbors
   */
  List<? extends KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k);

  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param obj Query object
   * @param k Number of neighbors requested
   * @return neighbors
   */
  KNNList getKNNForObject(O obj, int k);
}