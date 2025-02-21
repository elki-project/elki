/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2024
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
package elki.database.query.knn;

import elki.database.ids.KNNList;

/**
 * Wrapper class to allow using an Euclidean search tree with squared Euclidean
 * distance.
 * 
 * @author Erich Schubert
 *
 * @param <O> Data type
 */
public class SquaredKNNSearcher<O> implements KNNSearcher<O> {
  /**
   * Wrapped searcher
   */
  private KNNSearcher<O> inner;

  /**
   * Constructor.
   *
   * @param inner Inner searcher
   */
  public SquaredKNNSearcher(KNNSearcher<O> inner) {
    this.inner = inner;
  }

  @Override
  public KNNList getKNN(O query, int k) {
    return inner.getKNN(query, k).map(x -> x * x);
  }
}
