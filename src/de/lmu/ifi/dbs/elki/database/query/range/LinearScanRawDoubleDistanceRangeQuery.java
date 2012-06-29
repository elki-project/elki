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

import java.util.Collections;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
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
public class LinearScanRawDoubleDistanceRangeQuery<O> extends LinearScanRangeQuery<O, DoubleDistance> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanRawDoubleDistanceRangeQuery(DistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
  }

  @Override
  public DistanceDBIDResult<DoubleDistance> getRangeForDBID(DBIDRef id, DoubleDistance range) {
    if(distanceQuery instanceof PrimitiveDistanceQuery && distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
      @SuppressWarnings("unchecked")
      PrimitiveDoubleDistanceFunction<O> rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
      double epsilon = range.doubleValue();

      O qo = relation.get(id);
      GenericDistanceDBIDList<DoubleDistance> result = new GenericDistanceDBIDList<DoubleDistance>();
      for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        DBID currentID = iter.getDBID();
        double doubleDistance = rawdist.doubleDistance(qo, relation.get(currentID));
        if(doubleDistance <= epsilon) {
          result.add(new DoubleDistanceResultPair(doubleDistance, currentID));
        }
      }
      Collections.sort(result);
      return result;
    }
    else {
      return super.getRangeForDBID(id, range);
    }
  }

  @Override
  public DistanceDBIDResult<DoubleDistance> getRangeForObject(O obj, DoubleDistance range) {
    if(distanceQuery instanceof PrimitiveDistanceQuery && distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
      @SuppressWarnings("unchecked")
      PrimitiveDoubleDistanceFunction<O> rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
      double epsilon = range.doubleValue();

      GenericDistanceDBIDList<DoubleDistance> result = new GenericDistanceDBIDList<DoubleDistance>();
      for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        DBID currentID = iter.getDBID();
        double doubleDistance = rawdist.doubleDistance(obj, relation.get(currentID));
        if(doubleDistance <= epsilon) {
          result.add(new DoubleDistanceResultPair(doubleDistance, currentID));
        }
      }
      Collections.sort(result);
      return result;
    }
    else {
      return super.getRangeForObject(obj, range);
    }
  }
}