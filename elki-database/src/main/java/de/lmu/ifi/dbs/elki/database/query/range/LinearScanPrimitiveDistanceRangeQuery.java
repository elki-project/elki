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
package de.lmu.ifi.dbs.elki.database.query.range;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/**
 * Default linear scan range query class.
 * 
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
public class LinearScanPrimitiveDistanceRangeQuery<O> extends AbstractDistanceRangeQuery<O> {
  /**
   * Unboxed distance function.
   */
  private PrimitiveDistanceFunction<? super O> rawdist;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceRangeQuery(PrimitiveDistanceQuery<O> distanceQuery) {
    super(distanceQuery);
    rawdist = distanceQuery.getDistanceFunction();
  }

  @Override
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    final Relation<? extends O> relation = getRelation();
    // Note: subtle optimization. Get "id" only once!
    final O obj = relation.get(id);
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    linearScan(relation, relation.iterDBIDs(), obj, range, result);
    result.sort();
    return result;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    final Relation<? extends O> relation = getRelation();
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    linearScan(relation, relation.iterDBIDs(), obj, range, result);
    result.sort();
    return result;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    final Relation<? extends O> relation = getRelation();
    linearScan(relation, relation.iterDBIDs(), relation.get(id), range, neighbors);
  }

  @Override
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList neighbors) {
    final Relation<? extends O> relation = getRelation();
    linearScan(relation, relation.iterDBIDs(), obj, range, neighbors);
  }

  /**
   * Main loop for linear scan,
   * 
   * @param relation Data relation
   * @param iter Iterator
   * @param obj Query object
   * @param range Query radius
   * @param result Output data structure
   */
  private void linearScan(Relation<? extends O> relation, DBIDIter iter, O obj, double range, ModifiableDoubleDBIDList result) {
    final PrimitiveDistanceFunction<? super O> rawdist = this.rawdist;
    while(iter.valid()) {
      final double distance = rawdist.distance(obj, relation.get(iter));
      if(distance <= range) {
        result.add(distance, iter);
      }
      iter.advance();
    }
  }
}
