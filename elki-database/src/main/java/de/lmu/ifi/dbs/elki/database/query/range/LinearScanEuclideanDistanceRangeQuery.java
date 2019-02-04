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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import net.jafama.FastMath;

/**
 * Optimized linear scan for Euclidean distance range queries.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - SquaredEuclideanDistanceFunction
 * 
 * @param <O> Database object type
 */
public class LinearScanEuclideanDistanceRangeQuery<O extends NumberVector> extends LinearScanPrimitiveDistanceRangeQuery<O> {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanEuclideanDistanceRangeQuery(PrimitiveDistanceQuery<O> distanceQuery) {
    super(distanceQuery);
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
    final SquaredEuclideanDistanceFunction squared = SquaredEuclideanDistanceFunction.STATIC;
    // Avoid a loss in numerical precision when using the squared radius:
    final double upper = range * 1.0000001;
    // This should be more precise, but slower:
    // upper = MathUtil.floatToDoubleUpper((float)range);
    final double sqrange = upper * upper;
    while(iter.valid()) {
      final double sqdistance = squared.distance(obj, relation.get(iter));
      if(sqdistance <= sqrange) {
        final double dist = FastMath.sqrt(sqdistance);
        if(dist <= range) { // double check, as we increased the radius above
          result.add(dist, iter);
        }
      }
      iter.advance();
    }
  }
}
