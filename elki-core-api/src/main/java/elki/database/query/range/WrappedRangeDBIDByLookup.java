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

import elki.database.ids.DBIDRef;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.LinearScanQuery;
import elki.database.relation.Relation;

/**
 * Find radius neighbors by querying with the original object.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> relation object type
 */
public class WrappedRangeDBIDByLookup<O> implements RangeSearcher<DBIDRef> {
  /**
   * Data relation.
   */
  private Relation<? extends O> relation;

  /**
   * Inner radius searcher.
   */
  private RangeSearcher<O> inner;

  /**
   * Constructor.
   *
   * @param relation data relation
   * @param inner inner radius searcher
   */
  private WrappedRangeDBIDByLookup(Relation<? extends O> relation, RangeSearcher<O> inner) {
    super();
    this.relation = relation;
    this.inner = inner;
  }

  @Override
  public ModifiableDoubleDBIDList getRange(DBIDRef query, double range, ModifiableDoubleDBIDList result) {
    return inner.getRange(relation.get(query), range, result);
  }

  /**
   * Linear scan searcher.
   * 
   * @author Erich Schubert
   *
   * @param <O> relation object type
   */
  public static class Linear<O> extends WrappedRangeDBIDByLookup<O> implements LinearScanQuery {
    /**
     * Constructor.
     *
     * @param relation data relation
     * @param inner inner radius searcher
     */
    private Linear(Relation<? extends O> relation, RangeSearcher<O> inner) {
      super(relation, inner);
    }
  }

  /**
   * Wrap a query, if not {@code null}.
   *
   * @param <O> inner data type
   * @param relation Relation to get object from
   * @param inner inner query
   * @return wrapped query, or {@code null} if inner was {@code null}
   */
  public static <O> RangeSearcher<DBIDRef> wrap(Relation<? extends O> relation, RangeSearcher<O> inner) {
    return inner == null ? null : inner instanceof LinearScanQuery //
        ? new Linear<>(relation, inner) : new WrappedRangeDBIDByLookup<>(relation, inner);
  }
}
