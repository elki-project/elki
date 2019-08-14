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

import elki.database.ids.*;
import elki.database.query.LinearScanQuery;
import elki.database.query.distance.PrimitiveDistanceQuery;
import elki.database.relation.Relation;
import elki.distance.PrimitiveDistance;

/**
 * Default linear scan range query class.
 * <p>
 * Subtle optimization: for primitive distances, retrieve the query object only
 * once from the relation.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - PrimitiveDistanceQuery
 * 
 * @param <O> Database object type
 */
public class LinearScanPrimitiveDistanceRangeQuery<O> implements RangeQuery<O>, LinearScanQuery {
  /**
   * Unboxed distance function.
   */
  private PrimitiveDistance<? super O> rawdist;

  /**
   * Relation to query.
   */
  private Relation<? extends O> relation;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceRangeQuery(PrimitiveDistanceQuery<O> distanceQuery) {
    super();
    this.relation = distanceQuery.getRelation();
    rawdist = distanceQuery.getDistance();
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    return getRangeForObject(relation.get(id), range, result);
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
    final Relation<? extends O> relation = this.relation;
    final PrimitiveDistance<? super O> rawdist = this.rawdist;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double distance = rawdist.distance(obj, relation.get(iter));
      if(distance <= range) {
        result.add(distance, iter);
      }
    }
    return result;
  }
}
