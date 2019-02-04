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

import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract class for all M-Tree variants supporting processing of reverse
 * k-nearest neighbor queries by using the k-nn distances of the entries, where
 * k is less than or equal to the given parameter.
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @navassoc - - - MkTreeHeader
 * @composed - - - MkTreeSettings
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 * @param <S> the type of Settings used.
 */
public abstract class AbstractMkTreeUnified<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry, S extends MkTreeSettings<O, N, E>> extends AbstractMkTree<O, N, E, S> {
  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param pagefile Page file
   * @param settings Settings file
   */
  public AbstractMkTreeUnified(Relation<O> relation, PageFile<N> pagefile, S settings) {
    super(relation, pagefile, settings);
  }

  /**
   * @return a new {@link MkTreeHeader}
   */
  @Override
  protected TreeIndexHeader createHeader() {
    return new MkTreeHeader(getPageSize(), dirCapacity, leafCapacity, settings.kmax);
  }

  @Override
  public void insertAll(List<E> entries) {
    if (entries.isEmpty()) {
      return;
    }
    if (!initialized) {
      initialize(entries.get(0));
    }

    ModifiableDBIDs ids = DBIDUtil.newArray(entries.size());

    // insert sequentially
    for (E entry : entries) {
      ids.add(entry.getRoutingObjectID());
      // insert the object
      super.insert(entry, false);
    }

    // do batch nn
    Map<DBID, KNNList> knnLists = batchNN(getRoot(), ids, settings.kmax);

    // adjust the knn distances
    kNNdistanceAdjustment(getRootEntry(), knnLists);

    if (EXTRA_INTEGRITY_CHECKS) {
      getRoot().integrityCheck(this, getRootEntry());
    }
  }

  /**
   * Performs a distance adjustment in the subtree of the specified root entry.
   * 
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  protected abstract void kNNdistanceAdjustment(E entry, Map<DBID, KNNList> knnLists);

  /**
   * Get the value of k_max.
   * 
   * @return k_max value.
   */
  public int getKmax() {
    return settings.kmax;
  }
}
