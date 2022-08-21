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
package elki.database.query;

import elki.database.ids.DBIDRef;
import elki.database.ids.KNNList;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.relation.Relation;

/**
 * Find nearest neighbors by querying with the original object.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> relation object type
 */
public class WrappedPrioritySearchDBIDByLookup<O> implements PrioritySearcher<DBIDRef> {
  /**
   * Data relation.
   */
  private Relation<? extends O> relation;

  /**
   * Inner kNN searcher.
   */
  private PrioritySearcher<O> inner;

  /**
   * Constructor.
   *
   * @param relation data relation
   * @param inner inner kNN searcher
   */
  private WrappedPrioritySearchDBIDByLookup(Relation<? extends O> relation, PrioritySearcher<O> inner) {
    super();
    this.relation = relation;
    this.inner = inner;
  }

  @Override
  public ModifiableDoubleDBIDList getRange(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    return inner.getRange(relation.get(id), range, result);
  }

  @Override
  public KNNList getKNN(DBIDRef id, int k) {
    return inner.getKNN(relation.get(id), k);
  }

  @Override
  public PrioritySearcher<DBIDRef> search(DBIDRef query) {
    inner.search(relation.get(query));
    return this;
  }

  @Override
  public PrioritySearcher<DBIDRef> advance() {
    inner.advance();
    return this;
  }

  @Override
  public boolean valid() {
    return inner.valid();
  }

  @Override
  public PrioritySearcher<DBIDRef> decreaseCutoff(double threshold) {
    inner.decreaseCutoff(threshold);
    return this;
  }

  @Override
  public int internalGetIndex() {
    return inner.internalGetIndex();
  }

  @Override
  public double getApproximateDistance() {
    return inner.getApproximateDistance();
  }

  @Override
  public double getApproximateAccuracy() {
    return inner.getApproximateAccuracy();
  }

  @Override
  public double getLowerBound() {
    return inner.getLowerBound();
  }

  @Override
  public double getUpperBound() {
    return inner.getUpperBound();
  }

  @Override
  public double computeExactDistance() {
    return inner.computeExactDistance();
  }

  @Override
  public double allLowerBound() {
    return inner.allLowerBound();
  }

  /**
   * Linear scan searcher.
   * 
   * @author Erich Schubert
   *
   * @param <O> relation object type
   */
  public static class Linear<O> extends WrappedPrioritySearchDBIDByLookup<O> implements LinearScanQuery {
    /**
     * Constructor.
     *
     * @param relation data relation
     * @param inner inner kNN searcher
     */
    private Linear(Relation<? extends O> relation, PrioritySearcher<O> inner) {
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
  public static <O> PrioritySearcher<DBIDRef> wrap(Relation<? extends O> relation, PrioritySearcher<O> inner) {
    return inner == null ? null : inner instanceof LinearScanQuery //
        ? new Linear<>(relation, inner) : new WrappedPrioritySearchDBIDByLookup<>(relation, inner);
  }
}
