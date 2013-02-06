package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Instance for the query on a particular database.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractDistanceKNNQuery<O, D extends Distance<D>> extends AbstractDataBasedQuery<O> implements KNNQuery<O, D> {
  /**
   * Hold the distance function to be used.
   */
  protected DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance query used
   */
  public AbstractDistanceKNNQuery(DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery.getRelation());
    this.distanceQuery = distanceQuery;
  }

  @Override
  public List<? extends KNNList<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // throw new
    // UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
    // TODO: optimize
    List<KNNList<D>> ret = new ArrayList<>(ids.size());
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      ret.add(getKNNForDBID(iter, k));
    }
    return ret;
  }

  @Override
  public KNNList<D> getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  abstract public KNNList<D> getKNNForObject(O obj, int k);
}
