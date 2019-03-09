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
package elki.database.query.range;

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.LinearScanQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;

/**
 * Default linear scan range query class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DistanceQuery
 *
 * @param <O> Database object type
 */
public class LinearScanDistanceRangeQuery<O> extends AbstractDistanceRangeQuery<O> implements LinearScanQuery {
  /**
   * Constructor.
   *
   * @param distanceQuery Distance function to use
   */
  public LinearScanDistanceRangeQuery(DistanceQuery<O> distanceQuery) {
    super(distanceQuery);
  }

  @Override
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    final DistanceQuery<O> dq = distanceQuery;
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = getRelation().getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double currentDistance = dq.distance(id, iter);
      if(currentDistance <= range) {
        result.add(currentDistance, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    final DistanceQuery<O> dq = distanceQuery;
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = getRelation().getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double currentDistance = dq.distance(obj, iter);
      if(currentDistance <= range) {
        result.add(currentDistance, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    final DistanceQuery<O> dq = distanceQuery;
    for(DBIDIter iter = getRelation().iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentDistance = dq.distance(id, iter);
      if(currentDistance <= range) {
        neighbors.add(currentDistance, iter);
      }
    }
  }

  @Override
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList neighbors) {
    final Relation<? extends O> relation = getRelation();
    final DistanceQuery<O> dq = distanceQuery;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentDistance = dq.distance(obj, iter);
      if(currentDistance <= range) {
        neighbors.add(currentDistance, iter);
      }
    }
  }
}
