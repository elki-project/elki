package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
 * 
 * @apiviz.has MkTreeHeader oneway
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMkTreeUnified<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractMkTree<O, D, N, E> {
  /**
   * Holds the maximum value of k to support.
   */
  private int k_max;

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   * @param k_max Maximum value for k
   */
  public AbstractMkTreeUnified(PageFile<N> pagefile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(pagefile, distanceQuery, distanceFunction);
    this.k_max = k_max;
  }

  /**
   * @return a new {@link MkTreeHeader}
   */
  @Override
  protected TreeIndexHeader createHeader() {
    return new MkTreeHeader(getPageSize(), dirCapacity, leafCapacity, k_max);
  }

  @Override
  public void insertAll(List<E> entries) {
    if(entries.size() <= 0) {
      return;
    }
    if(!initialized) {
      initialize(entries.get(0));
    }

    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();
    ModifiableDBIDs ids = DBIDUtil.newArray(entries.size());

    // insert sequentially
    for(E entry : entries) {
      // create knnList for the object
      final DBID id = entry.getRoutingObjectID();

      ids.add(id);
      knnLists.put(id, KNNUtil.newHeap(distanceFunction, k_max));

      // insert the object
      super.insert(entry, false);
    }

    // do batch nn
    batchNN(getRoot(), ids, knnLists);

    // adjust the knn distances
    kNNdistanceAdjustment(getRootEntry(), knnLists);

    if(extraIntegrityChecks) {
      getRoot().integrityCheck(this, getRootEntry());
    }
  }

  /**
   * Performs a distance adjustment in the subtree of the specified root entry.
   * 
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  protected abstract void kNNdistanceAdjustment(E entry, Map<DBID, KNNHeap<D>> knnLists);

  /**
   * Get the value of k_max.
   * 
   * @return k_max value.
   */
  public int getKmax() {
    return k_max;
  }
}