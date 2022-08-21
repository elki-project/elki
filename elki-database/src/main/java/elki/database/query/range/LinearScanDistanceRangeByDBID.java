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
import elki.database.query.distance.DistanceQuery;

/**
 * Default linear scan range query class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DistanceQuery
 *
 * @param <O> relation object type
 */
public class LinearScanDistanceRangeByDBID<O> implements RangeSearcher<DBIDRef>, LinearScanQuery {
  /**
   * Distance to use.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   *
   * @param distanceQuery Distance function to use
   */
  public LinearScanDistanceRangeByDBID(DistanceQuery<O> distanceQuery) {
    super();
    this.distanceQuery = distanceQuery;
  }

  @Override
  public ModifiableDoubleDBIDList getRange(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    final DistanceQuery<O> dq = distanceQuery;
    for(DBIDIter iter = dq.getRelation().iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentDistance = dq.distance(id, iter);
      if(currentDistance <= range) {
        result.add(currentDistance, iter);
      }
    }
    return result;
  }
}
