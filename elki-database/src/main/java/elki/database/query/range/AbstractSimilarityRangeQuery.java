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

import elki.database.ids.DBIDRef;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;

/**
 * Abstract base class for range queries that use a similarity query in their
 * instance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Database object type
 */
public abstract class AbstractSimilarityRangeQuery<O> implements RangeQuery<O> {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * Hold the similarity function to be used.
   */
  final protected SimilarityQuery<O> simQuery;

  /**
   * Constructor.
   *
   * @param simQuery Similarity query
   */
  public AbstractSimilarityRangeQuery(SimilarityQuery<O> simQuery) {
    super();
    this.relation = simQuery.getRelation();
    this.simQuery = simQuery;
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    return getRangeForObject(relation.get(id), range, result);
  }
}
