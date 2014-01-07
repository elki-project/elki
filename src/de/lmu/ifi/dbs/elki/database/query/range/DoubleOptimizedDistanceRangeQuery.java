package de.lmu.ifi.dbs.elki.database.query.range;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.integer.DoubleDistanceIntegerDBIDList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Default linear scan range query class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDoubleDistanceFunction
 * 
 * @param <O> Database object type
 */
public class DoubleOptimizedDistanceRangeQuery<O> extends AbstractDistanceRangeQuery<O, DoubleDistance> implements LinearScanQuery {
  /**
   * Raw distance function.
   */
  PrimitiveDoubleDistanceFunction<? super O> rawdist;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  @SuppressWarnings("unchecked")
  public DoubleOptimizedDistanceRangeQuery(DistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
    if(!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
      throw new UnsupportedOperationException("DoubleOptimizedRangeQuery instantiated for non-PrimitiveDoubleDistanceFunction!");
    }
    rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
  }

  @Override
  public DistanceDBIDList<DoubleDistance> getRangeForDBID(DBIDRef id, DoubleDistance range) {
    final Relation<? extends O> relation = this.relation;
    DoubleDistanceIntegerDBIDList result = new DoubleDistanceIntegerDBIDList();
    linearScan(relation, relation.iterDBIDs(), rawdist, relation.get(id), range.doubleValue(), result);
    result.sort();
    return result;
  }

  @Override
  public DistanceDBIDList<DoubleDistance> getRangeForObject(O obj, DoubleDistance range) {
    DoubleDistanceIntegerDBIDList result = new DoubleDistanceIntegerDBIDList();
    linearScan(relation, relation.iterDBIDs(), rawdist, obj, range.doubleValue(), result);
    result.sort();
    return result;
  }

  private static <O> void linearScan(Relation<? extends O> relation, DBIDIter iter, PrimitiveDoubleDistanceFunction<? super O> rawdist, O obj, double range, ModifiableDoubleDistanceDBIDList result) {
    while(iter.valid()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(iter));
      if(doubleDistance <= range) {
        result.add(doubleDistance, iter);
      }
      iter.advance();
    }
  }
}
