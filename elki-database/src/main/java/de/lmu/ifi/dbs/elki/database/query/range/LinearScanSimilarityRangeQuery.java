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
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;

/**
 * Default linear scan range query class.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - SimilarityQuery
 *
 * @param <O> Database object type
 */
public class LinearScanSimilarityRangeQuery<O> extends AbstractSimilarityRangeQuery<O> implements LinearScanQuery {
  /**
   * Constructor.
   *
   * @param simQuery Similarity function to use
   */
  public LinearScanSimilarityRangeQuery(SimilarityQuery<O> simQuery) {
    super(simQuery);
  }

  @Override
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double currentSim = simQuery.similarity(id, iter);
      if(currentSim >= range) {
        result.add(currentSim, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      final double currentSim = simQuery.similarity(obj, iter);
      if(currentSim >= range) {
        result.add(currentSim, iter);
      }
    }
    result.sort();
    return result;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentSim = simQuery.similarity(id, iter);
      if(currentSim >= range) {
        neighbors.add(currentSim, iter);
      }
    }
  }

  @Override
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList neighbors) {
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentSim = simQuery.similarity(obj, iter);
      if(currentSim >= range) {
        neighbors.add(currentSim, iter);
      }
    }
  }
}
