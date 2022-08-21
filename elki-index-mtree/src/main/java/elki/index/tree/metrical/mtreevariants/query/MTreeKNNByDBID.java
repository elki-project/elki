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

import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.index.tree.DirectoryEntry;
import elki.index.tree.metrical.mtreevariants.AbstractMTree;
import elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import elki.index.tree.metrical.mtreevariants.MTreeEntry;
import elki.utilities.datastructures.heap.ComparableMinHeap;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - AbstractMTree
 * @assoc - - - MTreeSearchCandidate
 * 
 * @param <O> Object type
 */
public class MTreeKNNByDBID<O> implements KNNSearcher<DBIDRef> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, ?, ?, ?> index;

  /**
   * Hold the distance function to be used.
   */
  protected final DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MTreeKNNByDBID(AbstractMTree<O, ?, ?, ?> index, DistanceQuery<O> distanceQuery) {
    super();
    this.index = index;
    this.distanceQuery = distanceQuery;
  }

  @Override
  public KNNList getKNN(DBIDRef q, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one object has to be requested!");
    }
    index.statistics.countKNNQuery();

    KNNHeap knnList = DBIDUtil.newHeap(k);
    double d_k = Double.POSITIVE_INFINITY;

    final ComparableMinHeap<MTreeSearchCandidate> pq = new ComparableMinHeap<>();

    // Push the root node
    pq.add(new MTreeSearchCandidate(0., index.getRootID(), null, 0.));

    // search in tree
    while(!pq.isEmpty()) {
      MTreeSearchCandidate pqNode = pq.poll();
      if(knnList.size() >= k && pqNode.mindist > d_k) {
        break;
      }

      AbstractMTreeNode<?, ?, ?> node = index.getNode(pqNode.nodeID);
      DBID id_p = pqNode.routingObjectID;
      double d1 = pqNode.routingDistance;

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MTreeEntry entry = node.getEntry(i);
          double r_or = entry.getCoveringRadius();
          double d2 = id_p != null ? entry.getParentDistance() : 0.;
          if(Math.abs(d1 - d2) <= d_k + r_or) {
            DBID o_r = entry.getRoutingObjectID();
            double d3 = distanceQuery.distance(o_r, q);
            index.statistics.countDistanceCalculation();
            double d_min = Math.max(d3 - r_or, 0.);
            if(d_min <= d_k) {
              pq.add(new MTreeSearchCandidate(d_min, ((DirectoryEntry) entry).getPageID(), o_r, d3));
            }
          }
        }
      }
      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          MTreeEntry entry = node.getEntry(i);
          double d2 = id_p != null ? entry.getParentDistance() : 0.;
          if(Math.abs(d1 - d2) <= d_k) {
            DBID o_j = entry.getRoutingObjectID();
            double d3 = distanceQuery.distance(o_j, q);
            index.statistics.countDistanceCalculation();
            if(d3 <= d_k) {
              knnList.insert(d3, o_j);
              d_k = knnList.getKNNDistance();
            }
          }
        }
      }
    }
    return knnList.toKNNList();
  }
}
