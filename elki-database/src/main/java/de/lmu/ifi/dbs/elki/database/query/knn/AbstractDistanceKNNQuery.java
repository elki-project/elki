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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Instance for the query on a particular database.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractDistanceKNNQuery<O> implements KNNQuery<O> {
  /**
   * Hold the distance function to be used.
   */
  final protected DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance query used
   */
  public AbstractDistanceKNNQuery(DistanceQuery<O> distanceQuery) {
    super();
    this.distanceQuery = distanceQuery;
  }

  @Override
  public List<? extends KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // throw new
    // UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
    // TODO: optimize
    List<KNNList> ret = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      ret.add(getKNNForDBID(iter, k));
    }
    return ret;
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(getRelation().get(id), k);
  }

  /**
   * Get the relation to query.
   *
   * @return Relation
   */
  protected Relation<? extends O> getRelation() {
    return distanceQuery.getRelation();
  }

  @Override
  abstract public KNNList getKNNForObject(O obj, int k);
}
