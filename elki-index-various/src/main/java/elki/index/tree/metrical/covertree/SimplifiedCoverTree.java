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
package elki.index.tree.metrical.covertree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import elki.database.ids.*;
import elki.database.query.distance.DistancePrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.DistancePriorityIndex;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.utilities.Priority;
import elki.utilities.datastructures.heap.DoubleObjectMinHeap;

/**
 * Simplified cover tree data structure (in-memory). This is a <i>metrical</i>
 * data structure that is similar to the M-tree, but not as balanced and
 * disk-oriented. However, by not having these requirements it does not require
 * the expensive splitting procedures of M-tree.
 * <p>
 * This version does not store the distance to the parent, so it needs only
 * about 40% of the memory of {@link CoverTree} but does more distance
 * computations for search.
 * <p>
 * Reference:
 * <p>
 * A. Beygelzimer, S. Kakade, J. Langford<br>
 * Cover trees for nearest neighbor<br>
 * In Proc. 23rd Int. Conf. Machine Learning (ICML 2006)
 * <p>
 * TODO: allow insertions and removals, as in the original publication.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - CoverTreeRangeQuery
 * @has - - - CoverTreeKNNQuery
 */
@Priority(Priority.RECOMMENDED)
public class SimplifiedCoverTree<O> extends AbstractCoverTree<O> implements DistancePriorityIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SimplifiedCoverTree.class);

  /**
   * Tree root.
   */
  private Node root = null;

  /**
   * Constructor.
   *
   * @param relation data relation
   * @param distanceFunction distance function
   * @param expansion Expansion rate
   * @param truncate Truncate branches with less than this number of instances.
   */
  public SimplifiedCoverTree(Relation<O> relation, Distance<? super O> distanceFunction, double expansion, int truncate) {
    super(relation, distanceFunction, expansion, truncate);
  }

  /**
   * Node object.
   *
   * @author Erich Schubert
   */
  private static final class Node {
    /**
     * Objects in this node. Except for the first, which is the routing object.
     */
    ArrayModifiableDBIDs singletons;

    /**
     * Maximum distance to descendants.
     */
    double maxDist = 0.;

    /**
     * Child nodes.
     */
    List<Node> children;

    /**
     * Constructor.
     *
     * @param r Object.
     * @param maxDist Maximum distance to any descendant.
     */
    public Node(DBIDRef r, double maxDist) {
      this.singletons = DBIDUtil.newArray();
      this.singletons.add(r);
      this.children = new ArrayList<>();
      this.maxDist = maxDist;
    }

    /**
     * Constructor for leaf node.
     *
     * @param r Object.
     * @param maxDist Maximum distance to any descendant.
     * @param singletons Singletons.
     */
    public Node(DBIDRef r, double maxDist, DoubleDBIDList singletons) {
      assert (!singletons.contains(r));
      this.singletons = DBIDUtil.newArray(singletons.size() + 1);
      this.singletons.add(r);
      this.singletons.addDBIDs(singletons);
      this.children = Collections.emptyList();
      this.maxDist = maxDist;
    }
  }

  @Override
  public void initialize() {
    bulkLoad(relation.getDBIDs());
    if(LOG.isVerbose()) {
      int[] counts = new int[5];
      checkCoverTree(root, counts, 0);
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".nodes", counts[0]));
      LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".avg-depth", counts[1] / (double) counts[0]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".max-depth", counts[2]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".singletons", counts[3]));
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".entries", counts[4]));
    }
  }

  /**
   * Bulk-load the index.
   *
   * @param ids IDs to load
   */
  public void bulkLoad(DBIDs ids) {
    if(ids.size() == 0) {
      return;
    }
    assert (root == null) : "Tree already initialized.";
    DBIDIter it = ids.iter();
    DBID first = DBIDUtil.deref(it);
    // Compute distances to all neighbors:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList(ids.size() - 1);
    for(it.advance(); it.valid(); it.advance()) {
      candidates.add(distance(first, it), it);
    }
    root = bulkConstruct(first, Integer.MAX_VALUE, candidates);
  }

  /**
   * Bulk-load the cover tree.
   * <p>
   * This bulk-load is slightly simpler than the one used in the original
   * cover-tree source: We do not look back into the "far" set of candidates.
   *
   * @param cur Current routing object
   * @param maxScale Maximum scale
   * @param elems Candidates
   * @return Root node of subtree
   */
  protected Node bulkConstruct(DBIDRef cur, int maxScale, ModifiableDoubleDBIDList elems) {
    assert (!elems.contains(cur));
    final double max = maxDistance(elems);
    final int scale = Math.min(distToScale(max) - 1, maxScale);
    final int nextScale = scale - 1;
    // Leaf node, because points coincide, we are too deep, or have too few
    // elements remaining:
    if(max <= 0 || scale <= scaleBottom || elems.size() < truncate) {
      return new Node(cur, max, elems);
    }
    // Find neighbors in the cover of the current object:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList();
    excludeNotCovered(elems, scaleToDist(scale), candidates);
    // If no elements were not in the cover, build a compact tree:
    if(candidates.size() == 0) {
      LOG.warning("Scale not chosen appropriately? " + max + " " + scaleToDist(scale));
      return bulkConstruct(cur, nextScale, elems);
    }
    // We will have at least one other child, so build the parent:
    Node node = new Node(cur, max);
    // Routing element now is a singleton:
    final boolean curSingleton = elems.size() == 0;
    if(!curSingleton) {
      // Add node for the routing object:
      node.children.add(bulkConstruct(cur, nextScale, elems));
    }
    final double fmax = scaleToDist(nextScale);
    // Build additional cover nodes:
    for(DoubleDBIDListIter it = candidates.iter(); it.valid();) {
      assert (it.getOffset() == 0);
      DBID t = DBIDUtil.deref(it);
      collectByCover(it, candidates, fmax, elems.clear());
      assert (DBIDUtil.equal(t, it)) : "First element in candidates must not change!";
      if(elems.size() == 0) { // Singleton
        node.singletons.add(it);
      }
      else {
        // Build a full child node:
        node.children.add(bulkConstruct(it, nextScale, elems));
      }
      candidates.removeSwap(0);
    }
    assert (candidates.size() == 0);
    // Routing object is not yet handled:
    if(curSingleton && !node.children.isEmpty()) {
      node.singletons.add(cur); // Add as regular singleton.
    }
    // TODO: improve recycling of lists?
    return node;
  }

  /**
   * Collect some statistics on the tree.
   *
   * @param cur Current node
   * @param counts Counter set
   * @param depth Current depth
   */
  private void checkCoverTree(Node cur, int[] counts, int depth) {
    counts[0] += 1; // Node count
    counts[1] += depth; // Sum of depth
    counts[2] = depth > counts[2] ? depth : counts[2]; // Max depth
    counts[3] += cur.singletons.size() - 1;
    counts[4] += cur.singletons.size() - (cur.children.isEmpty() ? 0 : 1);
    if(!cur.children.isEmpty()) {
      ++depth;
      for(Node chi : cur.children) {
        checkCoverTree(chi, counts, depth);
      }
    }
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    Distance<? super O> distanceFunction = (Distance<? super O>) distanceQuery.getDistance();
    if(!this.distanceFunction.equals(distanceFunction)) {
      LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new CoverTreeRangeQuery();
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    Distance<? super O> distanceFunction = (Distance<? super O>) distanceQuery.getDistance();
    if(!this.distanceFunction.equals(distanceFunction)) {
      return null;
    }
    return new CoverTreeKNNQuery();
  }

  @Override
  public DistancePrioritySearcher<O> getPriorityQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    Distance<? super O> distanceFunction = (Distance<? super O>) distanceQuery.getDistance();
    if(!this.distanceFunction.equals(distanceFunction)) {
      LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    return new PrioritySearcher();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Range query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeRangeQuery implements RangeQuery<O> {
    @Override
    public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
      return getRangeForObject(relation.get(id), range, result);
    }

    @Override
    public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
      ArrayList<Node> open = new ArrayList<Node>(); // LIFO stack
      open.add(root);
      DBIDVar r = DBIDUtil.newVar();
      while(!open.isEmpty()) {
        final Node cur = open.remove(open.size() - 1); // pop()
        cur.singletons.assignVar(0, r);
        final double d = distance(obj, r);
        // Covered area not in range (metric assumption!):
        if(d - cur.maxDist > range) {
          continue;
        }
        if(!cur.children.isEmpty()) { // Inner node:
          for(int i = 0, l = cur.children.size(); i < l; i++) {
            open.add(cur.children.get(i));
          }
        }
        else { // Leaf node
          // Consider routing object, too:
          if(d <= range) {
            result.add(d, r); // First element is a candidate now
          }
        }
        // For remaining singletons, compute the distances:
        for(int i = 1, l = cur.singletons.size(); i < l; i++) {
          cur.singletons.assignVar(i, r);
          final double d2 = distance(obj, r);
          if(d2 <= range) {
            result.add(d2, r);
          }
        }
      }
      return result;
    }
  }

  /**
   * KNN Query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeKNNQuery implements KNNQuery<O> {
    @Override
    public KNNList getKNNForDBID(DBIDRef id, int k) {
      return getKNNForObject(relation.get(id), k);
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      if(k < 1) {
        throw new IllegalArgumentException("At least one object has to be requested!");
      }

      KNNHeap knnList = DBIDUtil.newHeap(k);
      double d_k = Double.POSITIVE_INFINITY;

      final DoubleObjectMinHeap<Node> pq = new DoubleObjectMinHeap<>();

      // Push the root node
      final double rootdist = distance(obj, root.singletons.iter());
      pq.add(rootdist - root.maxDist, root);

      // search in tree
      while(!pq.isEmpty()) {
        final Node cur = pq.peekValue();
        final double prio = pq.peekKey(); // Minimum distance to cover
        final double d = prio + cur.maxDist; // Restore distance to center.
        pq.poll(); // Remove

        if(knnList.size() >= k && prio > d_k) {
          continue;
        }

        final DBIDIter it = cur.singletons.iter();

        if(!cur.children.isEmpty()) { // Inner node:
          for(Node c : cur.children) {
            final DBIDIter f = c.singletons.iter();
            final double dist = DBIDUtil.equal(f, it) ? d : distance(obj, f);
            final double newprio = dist - c.maxDist; // Minimum distance
            if(newprio <= d_k) {
              pq.add(newprio, c);
            }
          }
        }
        else { // Leaf node
          // Consider routing object, too:
          if(d <= d_k) {
            d_k = knnList.insert(d, it); // First element is a candidate now
          }
        }
        it.advance(); // Skip routing object.
        // For remaining singletons, compute the distances:
        while(it.valid()) {
          final double d2 = distance(obj, it);
          if(d2 <= d_k) {
            d_k = knnList.insert(d2, it);
          }
          it.advance();
        }
      }
      return knnList.toKNNList();
    }
  }

  /**
   * Priority query class.
   *
   * @author Erich Schubert
   */
  public class PrioritySearcher implements DistancePrioritySearcher<O> {
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
    DoubleObjectMinHeap<Node> pq = new DoubleObjectMinHeap<>();

    /**
     * Candidates
     */
    DBIDArrayIter candidates = EmptyDBIDs.EMPTY_ITERATOR;

    /**
     * Distance to routing object.
     */
    double routingDist;

    /**
     * Maximum distance of current node.
     */
    double maxDist;

    /**
     * Constructor.
     */
    public PrioritySearcher() {
      super();
    }

    @Override
    public PrioritySearcher search(O query) {
      this.query = query;
      this.threshold = Double.POSITIVE_INFINITY;
      pq.clear();
      // Push the root node to the heap.
      final double rootdist = distance(query, root.singletons.iter());
      pq.add(rootdist - root.maxDist, root);
      advance(); // Find first
      return this;
    }

    @Override
    public PrioritySearcher search(DBIDRef query) {
      // FIXME: support using DBIDs only.
      return search(relation.get(query));
    }

    @Override
    public PrioritySearcher decreaseCutoff(double threshold) {
      assert threshold <= this.threshold;
      this.threshold = threshold;
      return this;
    }

    @Override
    public boolean valid() {
      return candidates.valid();
    }

    @Override
    public PrioritySearcher advance() {
      // Advance the main iterator, if defined:
      if(candidates.valid()) {
        candidates.advance();
      }
      // First try the singletons
      // These aren't the best candidates usually, but we don't want to have to
      // manage them and their bounds in the heap. If we do this locally, we get
      // upper and lower bounds easily.
      do {
        // Pruning with lower bound:
        if(candidates.valid()) {
          return this;
        }
      }
      while(advanceQueue()); // Try next node
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
      final double prio = pq.peekKey(); // Minimum distance to cover
      if(prio > threshold) {
        pq.clear();
        return false;
      }
      final Node cur = pq.peekValue();
      routingDist = prio + cur.maxDist; // Restore distance to center.
      maxDist = cur.maxDist; // Accuracy for bounds
      candidates = cur.singletons.iter(); // Routing object initially
      pq.poll(); // Remove

      // Add child nodes to priority queue:
      for(Node c : cur.children) {
        final DBIDIter f = c.singletons.iter(); // Routing object
        final double dist = DBIDUtil.equal(f, candidates) ? routingDist : distance(query, f);
        final double newprio = dist - c.maxDist; // Minimum distance
        if(newprio <= threshold) {
          pq.add(newprio, c);
        }
      }
      if(!cur.children.isEmpty()) {
        candidates.advance(); // Skip routing object (also in children)
      }
      return true;
    }

    @Override
    public double getApproximateDistance() {
      return routingDist;
    }

    @Override
    public double getApproximateAccuracy() {
      return candidates.getOffset() == 0 ? 0. : maxDist;
    }

    @Override
    public double getLowerBound() {
      return candidates.getOffset() == 0 ? routingDist : routingDist - maxDist;
    }

    @Override
    public double getUpperBound() {
      return candidates.getOffset() == 0 ? routingDist : routingDist + maxDist;
    }

    @Override
    public double computeExactDistance() {
      return candidates.getOffset() == 0 ? routingDist : distance(query, candidates);
    }

    @Override
    public int internalGetIndex() {
      return candidates.internalGetIndex();
    }

    @Override
    public O getCandidate() {
      return relation.get(this);
    }
  }

  /**
   * Index factory.
   *
   * @author Erich Schubert
   *
   * @has - - - SimplifiedCoverTree
   *
   * @param <O> Object type
   */
  public static class Factory<O> extends AbstractCoverTree.Factory<O> {
    /**
     * Constructor.
     *
     * @param distanceFunction Distance function
     * @param expansion Expansion rate
     * @param truncate Truncate branches with less than this number of
     *        instances.
     */
    public Factory(Distance<? super O> distanceFunction, double expansion, int truncate) {
      super(distanceFunction, expansion, truncate);
    }

    @Override
    public SimplifiedCoverTree<O> instantiate(Relation<O> relation) {
      return new SimplifiedCoverTree<O>(relation, distanceFunction, expansion, truncate);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends AbstractCoverTree.Factory.Par<O> {
      @Override
      public SimplifiedCoverTree.Factory<O> make() {
        return new SimplifiedCoverTree.Factory<>(distanceFunction, expansion, truncate);
      }
    }
  }
}
