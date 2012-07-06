package de.lmu.ifi.dbs.elki.database.query.rknn;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract reverse kNN Query interface.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DistanceResultPair oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface RKNNQuery<O, D extends Distance<D>> extends DatabaseQuery {
  /**
   * Get the reverse k nearest neighbors for a particular id.
   * 
   * @param id query object ID
   * @param k number of neighbors requested
   * @return reverse k nearest neighbors
   */
  public DistanceDBIDResult<D> getRKNNForDBID(DBIDRef id, int k);

  /**
   * Get the reverse k nearest neighbors for a particular object.
   * 
   * @param obj query object instance
   * @param k number of neighbors requested
   * @return reverse k nearest neighbors
   */
  public DistanceDBIDResult<D> getRKNNForObject(O obj, int k);

  /**
   * Bulk query method for reverse k nearest neighbors for ids.
   * 
   * @param ids query object IDs
   * @param k number of neighbors requested
   * @return reverse k nearest neighbors
   */
  public List<? extends DistanceDBIDResult<D>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k);
}