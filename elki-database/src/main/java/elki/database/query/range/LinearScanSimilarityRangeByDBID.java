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

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.LinearScanQuery;
import elki.database.query.similarity.SimilarityQuery;

/**
 * Default linear scan range query class.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - SimilarityQuery
 *
 * @param <O> relation object type
 */
public class LinearScanSimilarityRangeByDBID<O> implements RangeSearcher<DBIDRef>, LinearScanQuery {
  /**
   * Hold the similarity function to be used.
   */
  protected final SimilarityQuery<O> simQuery;

  /**
   * Constructor.
   *
   * @param simQuery Similarity function to use
   */
  public LinearScanSimilarityRangeByDBID(SimilarityQuery<O> simQuery) {
    super();
    this.simQuery = simQuery;
  }

  @Override
  public ModifiableDoubleDBIDList getRange(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    for(DBIDIter iter = simQuery.getRelation().iterDBIDs(); iter.valid(); iter.advance()) {
      final double currentSim = simQuery.similarity(id, iter);
      if(currentSim >= range) {
        result.add(currentSim, iter);
      }
    }
    return result;
  }
}
