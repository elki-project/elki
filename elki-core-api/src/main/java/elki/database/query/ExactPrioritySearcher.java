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

import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDHeap;

/**
 * Priority searcher that refines all objects to their exact distances,
 * using another priority searcher inside to provide candidates. This class
 * allows accessing the kNN in increasing order (without storing all of them,
 * depending on the index).
 * <p>
 * We experimented with a queue of elements where only a lower bound of the
 * distance was available, and tried to lazily refine them, but there would only
 * be one element in this queue by the allLowerBound requirement on the inner
 * query.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type
 */
public class ExactPrioritySearcher<O> implements PrioritySearcher<O> {
  /**
   * Nested priority searcher
   */
  private PrioritySearcher<O> inner;

  /**
   * Distance threshold
   */
  private double threshold = Double.POSITIVE_INFINITY;

  /**
   * Refined candidates.
   */
  private DoubleDBIDHeap exact = DBIDUtil.newMinHeap(19);

  /**
   * Constructor.
   *
   * @param inner Inner query
   */
  public ExactPrioritySearcher(PrioritySearcher<O> inner) {
    this.inner = inner;
  }

  @Override
  public PrioritySearcher<O> search(O query) {
    inner.search(query);
    threshold = Double.POSITIVE_INFINITY;
    return advance();
  }

  @Override
  public PrioritySearcher<O> decreaseCutoff(double threshold) {
    inner.decreaseCutoff(threshold);
    this.threshold = threshold;
    return this;
  }

  @Override
  public double getApproximateDistance() {
    return exact.isEmpty() ? Double.NaN : exact.peekKey();
  }

  @Override
  public double getApproximateAccuracy() {
    return 0.;
  }

  @Override
  public double getLowerBound() {
    return exact.isEmpty() ? Double.NaN : exact.peekKey();
  }

  @Override
  public double getUpperBound() {
    return exact.isEmpty() ? Double.NaN : exact.peekKey();
  }

  @Override
  public double computeExactDistance() {
    return exact.isEmpty() ? Double.NaN : exact.peekKey();
  }

  @Override
  public int internalGetIndex() {
    return exact.internalGetIndex();
  }

  @Override
  public double allLowerBound() {
    return exact.isEmpty() ? Double.NaN : exact.peekKey();
  }

  @Override
  public boolean valid() {
    return !exact.isEmpty();
  }

  @Override
  public PrioritySearcher<O> advance() {
    if(!exact.isEmpty()) {
      exact.poll();
    }
    // First discard if the threshold permits:
    if(!exact.isEmpty() && exact.peekKey() > threshold) {
      exact.clear();
    }
    double ebound = exact.isEmpty() ? Double.POSITIVE_INFINITY : exact.peekKey();
    // Get new candidates from the nested query
    while(inner.valid() && inner.allLowerBound() < ebound) {
      double u = inner.getLowerBound(), l = inner.getUpperBound();
      ebound = exact.insert(u == l ? u : inner.computeExactDistance(), inner);
      inner.advance();
    }
    return this;
  }
}
