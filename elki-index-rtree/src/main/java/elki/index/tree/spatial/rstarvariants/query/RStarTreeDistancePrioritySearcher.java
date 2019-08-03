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
package elki.index.tree.spatial.rstarvariants.query;

import elki.data.spatial.SpatialComparable;
import elki.database.ids.*;
import elki.database.query.DistancePrioritySearcher;
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
 *
 * @assoc - - - AbstractRStarTree
 * @assoc - - - SpatialPrimitiveDistance
 * @assoc - - - DoubleDistanceSearchCandidate
 */
public class RStarTreeDistancePrioritySearcher<O extends SpatialComparable> implements DistancePrioritySearcher<O> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?, ?> tree;

  /**
   * Spatial primitive distance function.
   */
  protected final SpatialPrimitiveDistance<? super O> distanceFunction;

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
   * @param distanceFunction Distance function
   */
  public RStarTreeDistancePrioritySearcher(AbstractRStarTree<?, ?, ?> tree, Relation<? extends O> relation, SpatialPrimitiveDistance<? super O> distanceFunction) {
    super();
    this.relation = relation;
    this.tree = tree;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    KNNHeap heap = DBIDUtil.newHeap(k);
    double threshold = Double.POSITIVE_INFINITY;
    for(DistancePrioritySearcher<O> iter = search(obj); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= threshold) {
        iter.decreaseCutoff(threshold = heap.insert(dist, iter));
      }
    }
    return heap.toKNNList();
  }

  @Override
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    ModifiableDoubleDBIDList ret = DBIDUtil.newDistanceDBIDList();
    for(DistancePrioritySearcher<O> iter = search(id, range); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= range) {
        ret.add(dist, iter);
      }
    }
    ret.sort();
    return ret;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    ModifiableDoubleDBIDList ret = DBIDUtil.newDistanceDBIDList();
    getRangeForObject(obj, range, ret);
    ret.sort();
    return ret;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    getRangeForObject(relation.get(id), range, neighbors);
  }

  @Override
  public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
    for(DistancePrioritySearcher<O> iter = search(obj, range); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= range) {
        result.add(dist, iter);
      }
    }
  }

  @Override
  public RStarTreeDistancePrioritySearcher<O> search(DBIDRef query) {
    return search(relation.get(query));
  }

  @Override
  public RStarTreeDistancePrioritySearcher<O> search(O query) {
    this.query = query;
    this.threshold = Double.POSITIVE_INFINITY;
    pq.clear();
    // Push the root node to the heap.
    double rootdist = distanceFunction.minDist(query, tree.getRootEntry());
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
  public O getCandidate() {
    return relation.get(this);
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
        double distance = distanceFunction.minDist(query, entry);
        tree.statistics.countDistanceCalculation();
        if(distance <= threshold) {
          pq.add(distance, entry.getPageID());
        }
      }
      node = null;
    }
    return true;
  }

  @Override
  public double getLowerBound() {
    assert valid();
    return mindist;
  }

  @Override
  public double computeExactDistance() {
    assert valid();
    tree.statistics.countDistanceCalculation();
    return distanceFunction.minDist(query, node.getEntry(childnr));
  }

  @Override
  public int internalGetIndex() {
    assert valid();
    SpatialPointLeafEntry entry = (SpatialPointLeafEntry) node.getEntry(childnr);
    return entry.getDBID().internalGetIndex();
  }
}
