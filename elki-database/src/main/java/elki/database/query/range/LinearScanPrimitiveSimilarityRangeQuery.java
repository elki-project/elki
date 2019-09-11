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
package elki.database.query.range;

import elki.database.ids.*;
import elki.database.query.similarity.PrimitiveSimilarityQuery;
import elki.database.relation.Relation;
import elki.similarity.PrimitiveSimilarity;

/**
 * Default linear scan range query class.
 * <p>
 * Subtle optimization: for primitive similarities, retrieve the query object
 * only once from the relation.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @assoc - - - PrimitiveSimilarityQuery
 *
 * @param <O> Database object type
 */
public class LinearScanPrimitiveSimilarityRangeQuery<O> extends AbstractSimilarityRangeQuery<O> {
  /**
   * Unboxed similarity function.
   */
  private PrimitiveSimilarity<? super O> rawsim;

  /**
   * Constructor.
   *
   * @param similarityQuery Similarity function to use
   */
  public LinearScanPrimitiveSimilarityRangeQuery(PrimitiveSimilarityQuery<O> similarityQuery) {
    super(similarityQuery);
    rawsim = similarityQuery.getSimilarity();
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    return getRangeForObject(relation.get(id), range, result);
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
    final PrimitiveSimilarity<? super O> sim = this.rawsim;
    final Relation<? extends O> relation = this.relation;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double similarity = sim.similarity(obj, relation.get(iter));
      if(similarity >= range) {
        result.add(similarity, iter);
      }
    }
    return result;
  }
}
