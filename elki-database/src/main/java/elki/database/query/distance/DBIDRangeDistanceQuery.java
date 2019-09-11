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
package elki.database.query.distance;

import elki.database.ids.DBID;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.relation.Relation;
import elki.distance.DBIDDistance;
import elki.distance.DBIDRangeDistance;

/**
 * Run a distance query based on DBIDRanges
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - DBIDRangeDistance
 */
public class DBIDRangeDistanceQuery extends DBIDDistanceQuery {
  /**
   * The distance function we use.
   */
  final protected DBIDRangeDistance distanceFunction;

  /**
   * The DBID range we are accessing.
   */
  final protected DBIDRange range;

  /**
   * Constructor.
   *
   * @param relation Database to use.
   * @param distanceFunction Our distance function
   */
  public DBIDRangeDistanceQuery(Relation<DBID> relation, DBIDRangeDistance distanceFunction) {
    super(relation, distanceFunction);
    this.range = DBIDUtil.assertRange(relation.getDBIDs());
    distanceFunction.checkRange(this.range);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public double distance(DBIDRef id1, DBIDRef id2) {
    return distanceFunction.distance(range.getOffset(id1), range.getOffset(id2));
  }

  @Override
  public DBIDDistance getDistance() {
    return distanceFunction;
  }
}
