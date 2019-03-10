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

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.similarity.PrimitiveSimilarityQuery;
import elki.database.relation.Relation;
import elki.similarity.PrimitiveSimilarity;

/**
 * Default linear scan range query class.
 *
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
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    // Note: subtle optimization. Get "id" only once!
    final O obj = relation.get(id);
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    linearScan(relation, relation.iterDBIDs(), obj, range, result);
    result.sort();
    return result;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    linearScan(relation, relation.iterDBIDs(), obj, range, result);
    result.sort();
    return result;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    linearScan(relation, relation.iterDBIDs(), relation.get(id), range, neighbors);
  }

  @Override
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList neighbors) {
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
    while(iter.valid()) {
      final double similarity = rawsim.similarity(obj, relation.get(iter));
      if(similarity >= range) {
        result.add(similarity, iter);
      }
      iter.advance();
    }
  }
}