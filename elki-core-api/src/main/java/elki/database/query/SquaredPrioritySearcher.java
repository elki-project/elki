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

/**
 * Wrapper priority searcher for squared Euclidean distance that used an
 * Euclidean index, which allows the triangle inequality to be used.
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public class SquaredPrioritySearcher<O> implements PrioritySearcher<O> {
  /**
   * Nested priority searcher
   */
  private PrioritySearcher<O> inner;

  /**
   * Constructor.
   *
   * @param inner Inner query
   */
  public SquaredPrioritySearcher(PrioritySearcher<O> inner) {
    this.inner = inner;
  }

  @Override
  public PrioritySearcher<O> search(O query) {
    inner.search(query);
    return this;
  }
  
  @Override
  public PrioritySearcher<O> decreaseCutoff(double threshold) {
    inner.decreaseCutoff(threshold > 0 ? Math.sqrt(threshold) : 0.);
    return this;
  }

  @Override
  public PrioritySearcher<O> increaseSkip(double threshold) {
    return inner.increaseSkip(threshold > 0 ? Math.sqrt(threshold) : 0.);
  }

  static private final double square(double d) {
    return d * d;
  }

  @Override
  public double getApproximateDistance() {
    return square(inner.getApproximateDistance());
  }

  @Override
  public double getApproximateAccuracy() {
    return square(inner.getApproximateAccuracy());
  }

  @Override
  public double getLowerBound() {
    return square(inner.getLowerBound());
  }

  @Override
  public double getUpperBound() {
    return square(inner.getUpperBound());
  }

  @Override
  public double computeExactDistance() {
    // TODO: use SquaredEuclideanDistance
    return square(inner.computeExactDistance());
  }

  @Override
  public int internalGetIndex() {
    return inner.internalGetIndex();
  }

  @Override
  public double allLowerBound() {
    return square(inner.allLowerBound());
  }

  @Override
  public boolean valid() {
    return inner.valid();
  }

  @Override
  public PrioritySearcher<O> advance() {
    inner.advance();
    return this;
  }
}
