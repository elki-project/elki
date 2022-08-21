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
package elki.index.tree.spatial.rstarvariants.query;

import elki.data.spatial.SpatialComparable;
import elki.database.ids.DBIDUtil;
import elki.database.ids.KNNHeap;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
import elki.index.tree.spatial.SpatialDirectoryEntry;
import elki.index.tree.spatial.SpatialPointLeafEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;
import elki.utilities.documentation.Reference;

/**
 * Instance of a KNN query for a particular spatial index.
 * <p>
 * Reference:
 * <p>
 * G. R. Hjaltason, H. Samet<br>
 * Ranking in spatial databases<br>
 * 4th Symp. Advances in Spatial Databases (SSD'95)
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - AbstractRStarTree
 * @assoc - - - SpatialPrimitiveDistance
 */
@Reference(authors = "G. R. Hjaltason, H. Samet", //
    title = "Ranking in spatial databases", //
    booktitle = "4th Symp. Advances in Spatial Databases (SSD'95)", //
    url = "https://doi.org/10.1007/3-540-60159-7_6", //
    bibkey = "DBLP:conf/ssd/HjaltasonS95")
public class RStarTreeKNNSearcher<O extends SpatialComparable> implements KNNSearcher<O> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?, ?> tree;

  /**
   * Spatial primitive distance function.
   */
  protected final SpatialPrimitiveDistance<? super O> distance;

  /**
   * Relation we query.
   */
  protected Relation<? extends O> relation;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param relation Data relation to query
   * @param distance Distance function
   */
  public RStarTreeKNNSearcher(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation, SpatialPrimitiveDistance<? super O> distance) {
    super();
    this.relation = relation;
    this.tree = tree;
    this.distance = distance;
  }

  @Override
  public KNNList getKNN(O obj, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one neighbor has to be requested!");
    }
    tree.statistics.countKNNQuery();

    final KNNHeap knnList = DBIDUtil.newHeap(k);
    final DoubleIntegerMinHeap pq = new DoubleIntegerMinHeap(Math.min(knnList.getK() << 1, 21));

    // expand root
    double maxDist = expandNode(obj, knnList, pq, Double.MAX_VALUE, tree.getRootID());

    // search in tree
    while(!pq.isEmpty()) {
      double mindist = pq.peekKey();
      if(mindist > maxDist) {
        break;
      }
      int nodeID = pq.peekValue();
      pq.poll(); // Remove from heap.
      maxDist = expandNode(obj, knnList, pq, maxDist, nodeID);
    }
    return knnList.toKNNList();
  }

  private double expandNode(O object, KNNHeap knnList, DoubleIntegerMinHeap pq, double maxDist, final int nodeID) {
    AbstractRStarTreeNode<?, ?> node = tree.getNode(nodeID);
    // data node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialPointLeafEntry entry = (SpatialPointLeafEntry) node.getEntry(i);
        double dist = distance.minDist(entry, object);
        tree.statistics.countDistanceCalculation();
        maxDist = dist <= maxDist ? knnList.insert(dist, entry.getDBID()) : maxDist;
      }
    }
    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
        double dist = distance.minDist(entry, object);
        tree.statistics.countDistanceCalculation();
        // Greedy expand, bypassing the queue
        if(dist <= 0) {
          expandNode(object, knnList, pq, maxDist, entry.getPageID());
        }
        else if(dist <= maxDist) {
          pq.add(dist, entry.getPageID());
        }
      }
    }
    return maxDist;
  }
}
