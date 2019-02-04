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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MTreeKNNQuery;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract class for all M-Tree variants supporting processing of reverse
 * k-nearest neighbor queries by using the k-nn distances of the entries, where
 * k is less than or equal to the given parameter.
 * 
 * @author Elke Achtert
 * @since 0.2
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 * @param <S> the type of Settings kept.
 */
public abstract class AbstractMkTree<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry, S extends MTreeSettings<O, N, E>> extends AbstractMTree<O, N, E, S> {
  /**
   * Internal class for performing knn queries
   */
  protected KNNQuery<O> knnq;

  /**
   * Distance query to use.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param pagefile Page file
   * @param settings Settings class
   */
  public AbstractMkTree(Relation<O> relation, PageFile<N> pagefile, S settings) {
    super(pagefile, settings);
    // TODO: any way to un-tie MkTrees from relations?
    this.distanceQuery = getDistanceFunction().instantiate(relation);
    this.knnq = new MTreeKNNQuery<>(this, distanceQuery);
  }

  @Override
  public double distance(DBIDRef id1, DBIDRef id2) {
    if(id1 == null || id2 == null) {
      return Double.NaN;
    }
    if(DBIDUtil.equal(id1, id2)) {
      return 0.;
    }
    statistics.countDistanceCalculation();
    return distanceQuery.distance(id1, id2);
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param id the query object id
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public abstract DoubleDBIDList reverseKNNQuery(final DBIDRef id, int k);

  /**
   * Performs a batch k-nearest neighbor query for a list of query objects.
   * 
   * @param node the node representing the subtree on which the query should be
   *        performed
   * @param ids the ids of the query objects
   * @param kmax Maximum k value
   * 
   * @deprecated Change to use by-object NN lookups instead.
   */
  @Deprecated
  protected final Map<DBID, KNNList> batchNN(N node, DBIDs ids, int kmax) {
    Map<DBID, KNNList> res = new HashMap<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      res.put(id, knnq.getKNNForDBID(id, kmax));
    }
    return res;
  }
}
