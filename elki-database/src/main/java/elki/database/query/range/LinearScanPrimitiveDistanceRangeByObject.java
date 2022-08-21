/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
 * @param <O> relation object type
 */
public class LinearScanPrimitiveDistanceRangeByObject<O> implements RangeSearcher<O>, LinearScanQuery {
  /**
   * Distance query.
   */
  private PrimitiveDistanceQuery<O> distance;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceRangeByObject(PrimitiveDistanceQuery<O> distanceQuery) {
    super();
    this.distance = distanceQuery;
  }

  @Override
  public ModifiableDoubleDBIDList getRange(O obj, double range, ModifiableDoubleDBIDList result) {
    final PrimitiveDistance<? super O> rawdist = this.distance.getDistance();
    final Relation<? extends O> relation = this.distance.getRelation();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double distance = rawdist.distance(obj, relation.get(iter));
      if(distance <= range) {
        result.add(distance, iter);
      }
    }
    return result;
  }
}
