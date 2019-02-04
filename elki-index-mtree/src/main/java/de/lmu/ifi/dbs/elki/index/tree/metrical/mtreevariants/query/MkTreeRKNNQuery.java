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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.AbstractRKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;

/**
 * Instance of a rKNN query for a particular spatial index.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - AbstractMkTree
 */
public class MkTreeRKNNQuery<O> extends AbstractRKNNQuery<O> {
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
    super(distanceQuery);
    this.index = index;
  }

  @Override
  public DoubleDBIDList getRKNNForObject(O obj, int k) {
    throw new AbortException("Preprocessor KNN query only supports ID queries.");
  }

  @Override
  public DoubleDBIDList getRKNNForDBID(DBIDRef id, int k) {
    return index.reverseKNNQuery(id, k);
  }

  @Override
  public List<? extends DoubleDBIDList> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // TODO: implement
    throw new NotImplementedException();
  }
}