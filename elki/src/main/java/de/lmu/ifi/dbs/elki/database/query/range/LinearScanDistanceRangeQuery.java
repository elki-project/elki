package de.lmu.ifi.dbs.elki.database.query.range;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;

/**
 * Default linear scan range query class.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceQuery
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
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double currentDistance = distanceQuery.distance(id, iter);
      if(currentDistance <= range) {
        result.add(currentDistance, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double currentDistance = distanceQuery.distance(obj, iter);
      if(currentDistance <= range) {
        result.add(currentDistance, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentDistance = distanceQuery.distance(id, iter);
      if(currentDistance <= range) {
        neighbors.add(currentDistance, iter);
      }
    }
  }

  @Override
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList neighbors) {
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentDistance = distanceQuery.distance(obj, iter);
      if(currentDistance <= range) {
        neighbors.add(currentDistance, iter);
      }
    }
  }
}