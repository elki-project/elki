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
package elki.index.tree.metrical.mtreevariants.query;

import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.rknn.RKNNSearcher;
import elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;

/**
 * Instance of a rKNN query for a particular spatial index.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - AbstractMkTree
 */
public class MkTreeRKNNQuery<O> implements RKNNSearcher<DBIDRef> {
  /**
   * Distance query
   */
  protected DistanceQuery<O> distanceQuery;

  /**
   * The index to use
   */
  protected final AbstractMkTree<O, ?, ?, ?> index;

  /**
   * Constructor.
   *
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MkTreeRKNNQuery(AbstractMkTree<O, ?, ?, ?> index, DistanceQuery<O> distanceQuery) {
    super();
    this.distanceQuery = distanceQuery;
    this.index = index;
  }

  @Override
  public DoubleDBIDList getRKNN(DBIDRef id, int k) {
    return index.reverseKNNQuery(id, k);
  }
}
