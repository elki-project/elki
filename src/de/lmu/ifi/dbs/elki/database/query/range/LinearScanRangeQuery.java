package de.lmu.ifi.dbs.elki.database.query.range;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Default linear scan range query class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceQuery
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class LinearScanRangeQuery<O, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanRangeQuery(DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
  }

  @Override
  public DistanceDBIDResult<D> getRangeForDBID(DBIDRef id, D range) {
    GenericDistanceDBIDList<D> result = new GenericDistanceDBIDList<D>();
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      D currentDistance = distanceQuery.distance(id, iter);
      if(currentDistance.compareTo(range) <= 0) {
        result.add(currentDistance, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public DistanceDBIDResult<D> getRangeForObject(O obj, D range) {
    GenericDistanceDBIDList<D> result = new GenericDistanceDBIDList<D>();
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      D currentDistance = distanceQuery.distance(obj, iter);
      if(currentDistance.compareTo(range) <= 0) {
        result.add(currentDistance, iter);
      }
    }
    result.sort();
    return result;
  }
}