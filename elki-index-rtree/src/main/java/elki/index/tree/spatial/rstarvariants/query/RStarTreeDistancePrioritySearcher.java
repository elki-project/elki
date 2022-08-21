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
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.PrioritySearcher;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
import elki.index.tree.spatial.SpatialDirectoryEntry;
import elki.index.tree.spatial.SpatialPointLeafEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import elki.utilities.datastructures.heap.DoubleIntegerMinHeap;

/**
 * Instance of priority search for a particular spatial index.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @assoc - - - AbstractRStarTree
 * @assoc - - - SpatialPrimitiveDistance
 * @assoc - - - DoubleDistanceSearchCandidate
 */
public class RStarTreeDistancePrioritySearcher<O extends SpatialComparable> implements PrioritySearcher<O> {
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
   * Query object
   */
  O query;

  /**
   * Stopping distance threshold
   */
  double threshold = Double.POSITIVE_INFINITY;

  /**
   * Priority queue
   */
  // TODO: estimate necessary size?
  DoubleIntegerMinHeap pq = new DoubleIntegerMinHeap();

  /**
   * Current node
   */
  AbstractRStarTreeNode<?, ?> node;

  /**
   * Candidate within node
   */
  int childnr = 0;

  /**
   * Distance to current node
   */
  private double mindist;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param relation Data relation to query
   * @param distance Distance function
   */
  public RStarTreeDistancePrioritySearcher(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation, SpatialPrimitiveDistance<? super O> distance) {
    super();
    this.relation = relation;
    this.tree = tree;
    this.distance = distance;
  }

  @Override
  public KNNList getKNN(O obj, int k) {
    KNNHeap heap = DBIDUtil.newHeap(k);
    double threshold = Double.POSITIVE_INFINITY;
    for(PrioritySearcher<O> iter = search(obj); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= threshold) {
        iter.decreaseCutoff(threshold = heap.insert(dist, iter));
      }
    }
    return heap.toKNNList();
  }

  @Override
  public ModifiableDoubleDBIDList getRange(O obj, double range, ModifiableDoubleDBIDList result) {
    for(PrioritySearcher<O> iter = search(obj, range); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= range) {
        result.add(dist, iter);
      }
    }
    return result;
  }

  @Override
  public RStarTreeDistancePrioritySearcher<O> search(O query) {
    this.query = query;
    this.threshold = Double.POSITIVE_INFINITY;
    pq.clear();
    // Push the root node to the heap.
    double rootdist = distance.minDist(query, tree.getRootEntry());
    tree.statistics.countDistanceCalculation();
    pq.add(rootdist, tree.getRootID());
    advance(); // Find first
    return this;
  }

  @Override
  public RStarTreeDistancePrioritySearcher<O> decreaseCutoff(double threshold) {
    assert threshold <= this.threshold;
    this.threshold = threshold;
    return this;
  }

  @Override
  public boolean valid() {
    return node != null && childnr < node.getNumEntries();
  }

  @Override
  public RStarTreeDistancePrioritySearcher<O> advance() {
    // Advance the main iterator, if defined:
    if(node != null && ++childnr < node.getNumEntries()) {
      return this;
    }
    while(advanceQueue()) {
      if(node != null) {
        assert childnr == 0 && childnr < node.getNumEntries();
        break;
      }
    }
    return this;
  }

  /**
   * Expand the next node of the priority heap.
   */
  protected boolean advanceQueue() {
    if(pq.isEmpty()) {
      return false;
    }
    // Poll from heap (optimized, hence key and value separate):
    mindist = pq.peekKey(); // Minimum distance to cover
    if(mindist > threshold) {
      pq.clear();
      return false;
    }
    node = tree.getNode(pq.peekValue());
    pq.poll(); // Remove

    // data node
    if(node.isLeaf()) {
      childnr = 0;
    }
    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialDirectoryEntry entry = (SpatialDirectoryEntry) node.getEntry(i);
        double dist = distance.minDist(query, entry);
        tree.statistics.countDistanceCalculation();
        if(dist <= threshold) {
          pq.add(dist, entry.getPageID());
        }
      }
      node = null;
    }
    return true;
  }

  @Override
  public double getLowerBound() {
    return mindist;
  }

  @Override
  public double allLowerBound() {
    return mindist;
  }

  @Override
  public double computeExactDistance() {
    assert valid();
    tree.statistics.countDistanceCalculation();
    return distance.minDist(query, node.getEntry(childnr));
  }

  @Override
  public int internalGetIndex() {
    assert valid();
    SpatialPointLeafEntry entry = (SpatialPointLeafEntry) node.getEntry(childnr);
    return entry.getDBID().internalGetIndex();
  }
}
