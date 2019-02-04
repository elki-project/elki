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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;

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
public class MTreeKNNQuery<O> extends AbstractDistanceKNNQuery<O> {
  /**
   * The index to use
   */
  protected final AbstractMTree<O, ?, ?, ?> index;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MTreeKNNQuery(AbstractMTree<O, ?, ?, ?> index, DistanceQuery<O> distanceQuery) {
    super(distanceQuery);
    this.index = index;
  }

  @Override
  public KNNList getKNNForObject(O q, int k) {
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
          DBID o_r = entry.getRoutingObjectID();
          double r_or = entry.getCoveringRadius();
          double d2 = id_p != null ? entry.getParentDistance() : 0.;

          double diff = Math.abs(d1 - d2);

          double sum = d_k + r_or;

          if(diff <= sum) {
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
          DBID o_j = entry.getRoutingObjectID();

          double d2 = id_p != null ? entry.getParentDistance() : 0.;

          double diff = Math.abs(d1 - d2);

          if(diff <= d_k) {
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
