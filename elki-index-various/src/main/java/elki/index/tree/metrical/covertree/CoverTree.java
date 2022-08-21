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
package elki.index.tree.metrical.covertree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.DistancePriorityIndex;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.MathUtil;
import elki.utilities.Priority;
import elki.utilities.datastructures.heap.DoubleObjectMinHeap;
import elki.utilities.documentation.Reference;

/**
 * Cover tree data structure (in-memory). This is a <i>metrical</i> data
 * structure that is similar to the M-tree, but not as balanced and
 * disk-oriented. However, by not having these requirements it does not require
 * the expensive splitting procedures of M-tree.
 * <p>
 * This implementation contains some optimizations for Java: for example nodes
 * with no children are stored efficiently in the parent node. Sometimes this
 * also comes at some cost, as we currently do not push these onto a heap, but
 * rather process them along with the parent node. This may cause additional
 * distance computations (in particular for k nearest neighbor search), but also
 * saves some overhead in managing these candidates.
 * <p>
 * Reference:
 * <p>
 * A. Beygelzimer, S. Kakade, J. Langford<br>
 * Cover trees for nearest neighbor<br>
 * In Proc. 23rd Int. Conf. Machine Learning (ICML 2006)
 * <p>
 * This implementation uses metrical pruning, and keeps the distances to the
 * parent nodes. It thus needs more than twice the memory of
 * {@link SimplifiedCoverTree}, but computes fewer distances.
 * <p>
 * TODO: allow insertions and removals, as in the original publication.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - CoverTreeRangeSearcher
 * @has - - - CoverTreeKNNSearcher
 */
@Reference(authors = "A. Beygelzimer, S. Kakade, J. Langford", //
    title = "Cover trees for nearest neighbor", //
    booktitle = "In Proc. 23rd Int. Conf. Machine Learning (ICML 2006)", //
    url = "https://doi.org/10.1145/1143844.1143857", //
    bibkey = "DBLP:conf/icml/BeygelzimerKL06")
@Priority(Priority.RECOMMENDED)
public class CoverTree<O> extends AbstractCoverTree<O> implements DistancePriorityIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CoverTree.class);

  /**
   * Tree root.
   */
  private Node root = null;

  /**
   * Constructor.
   *
   * @param relation data relation
   * @param distance distance function
   * @param expansion Expansion rate
   * @param truncate Truncate branches with less than this number of instances
   */
  public CoverTree(Relation<O> relation, Distance<? super O> distance, double expansion, int truncate) {
    super(relation, distance, expansion, truncate);
  }

  /**
   * Constructor.
   *
   * @param relation data relation
   * @param distance distance function
   * @param truncate Truncation parameter
   */
  // Note: used from {@link EmpiricalQueryOptimizer} via reflection!
  public CoverTree(Relation<O> relation, Distance<? super O> distance, int truncate) {
    this(relation, distance, 1.3, truncate);
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
    ModifiableDoubleDBIDList singletons;

    /**
     * Maximum distance to descendants.
     */
    double maxDist = 0.;

    /**
     * Distance to parent.
     */
    double parentDist = 0.;

    /**
     * Child nodes.
     */
    List<Node> children;

    /**
     * Constructor.
     *
     * @param r Reference object
     * @param maxDist Maximum distance to any descendant
     * @param parentDist Distance from parent
     */
    public Node(DBIDRef r, double maxDist, double parentDist) {
      this.singletons = DBIDUtil.newDistanceDBIDList();
      this.singletons.add(0., r);
      this.children = new ArrayList<>();
      this.maxDist = maxDist;
      this.parentDist = parentDist;
    }

    /**
     * Constructor for leaf node.
     *
     * @param r Reference object
     * @param maxDist Maximum distance to any descendant
     * @param parentDist Distance from parent
     * @param singletons Singletons
     */
    public Node(DBIDRef r, double maxDist, double parentDist, DoubleDBIDList singletons) {
      assert !singletons.contains(r);
      this.singletons = DBIDUtil.newDistanceDBIDList(singletons.size() + 1);
      this.singletons.add(0., r);
      for(DoubleDBIDListIter it = singletons.iter(); it.valid(); it.advance()) {
        this.singletons.add(it.doubleValue(), it);
      }
      this.children = Collections.emptyList();
      this.maxDist = maxDist;
      this.parentDist = parentDist;
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
    if(ids.isEmpty()) {
      return;
    }
    assert root == null : "Tree already initialized.";
    DBIDIter it = ids.iter();
    DBID first = DBIDUtil.deref(it);
    // Compute distances to all neighbors:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList(ids.size() - 1);
    for(it.advance(); it.valid(); it.advance()) {
      candidates.add(distance(first, it), it);
    }
    root = bulkConstruct(first, Integer.MAX_VALUE, 0., candidates);
  }

  /**
   * Bulk-load the cover tree.
   * <p>
   * This bulk-load is slightly simpler than the one used in the original
   * cover-tree source: We do not look back into the "far" set of candidates.
   *
   * @param cur Current routing object
   * @param maxScale Maximum scale
   * @param parentDist Distance to parent element
   * @param elems Candidates
   * @return Root node of subtree
   */
  protected Node bulkConstruct(DBIDRef cur, int maxScale, double parentDist, ModifiableDoubleDBIDList elems) {
    final double max = maxDistance(elems);
    final int scale = Math.min(distToScale(max) - 1, maxScale);
    final int nextScale = scale - 1;
    // Leaf node, because points coincide, we are too deep, or have too few
    // elements remaining:
    if(max <= 0 || scale <= scaleBottom || elems.size() < truncate) {
      return new Node(cur, max, parentDist, elems);
    }
    // Find neighbors in the cover of the current object:
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList();
    excludeNotCovered(elems, scaleToDist(scale), candidates);
    // If no elements were not in the cover, build a compact tree:
    if(candidates.isEmpty()) {
      LOG.warning("Scale not chosen appropriately? " + max + " " + scaleToDist(scale));
      return bulkConstruct(cur, nextScale, parentDist, elems);
    }
    // We will have at least one other child, so build the parent:
    Node node = new Node(cur, max, parentDist);
    // Routing element now is a singleton:
    final boolean curSingleton = elems.isEmpty();
    if(!curSingleton) {
      // Add node for the routing object:
      node.children.add(bulkConstruct(cur, nextScale, 0, elems));
    }
    final double fmax = scaleToDist(nextScale);
    // Build additional cover nodes:
    for(DoubleDBIDListIter it = candidates.iter(); it.valid();) {
      assert it.getOffset() == 0;
      DBID t = DBIDUtil.deref(it);
      collectByCover(it, candidates, fmax, elems.clear());
      assert DBIDUtil.equal(t, it) : "First element in candidates must not change!";
      if(elems.isEmpty()) { // Singleton
        node.singletons.add(it.doubleValue(), it);
      }
      else {
        // Build a full child node:
        node.children.add(bulkConstruct(it, nextScale, it.doubleValue(), elems));
      }
      candidates.removeSwap(0);
    }
    assert candidates.isEmpty();
    // Routing object is not yet handled:
    if(curSingleton && !node.children.isEmpty()) {
      node.singletons.add(parentDist, cur); // Add as regular singleton.
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
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new CoverTreeRangeObjectSearcher() : null;
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new CoverTreeRangeDBIDSearcher() : null;
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new CoverTreePriorityObjectSearcher() : null;
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new CoverTreePriorityDBIDSearcher() : null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new CoverTreePriorityObjectSearcher() : null;
  }

  @Override
  public PrioritySearcher<DBIDRef> priorityByDBID(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new CoverTreePriorityDBIDSearcher() : null;
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
  public abstract class CoverTreeRangeSearcher {
    /**
     * LIFO stack of open nodes.
     */
    private ArrayList<Node> open = new ArrayList<>();

    /**
     * Temporary storage.
     */
    private DBIDVar tmp = DBIDUtil.newVar();

    /**
     * Compute distance to query object.
     *
     * @param it Candidate
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef it);

    /**
     * Perform the actual search.
     *
     * @param range Query range
     * @param result Output storage
     * @return result
     */
    protected ModifiableDoubleDBIDList doSearch(double range, ModifiableDoubleDBIDList result) {
      open.clear();
      open.add(root);
      while(!open.isEmpty()) {
        final Node cur = open.remove(open.size() - 1); // pop()
        final double d = queryDistance(cur.singletons.assignVar(0, tmp));
        // Covered area not in range (metric assumption):
        if(d - cur.maxDist > range) {
          continue;
        }
        if(!cur.children.isEmpty()) { // Inner node:
          for(Node c : cur.children) {
            // This only seems to reduce the number of distance computations
            // marginally, unfortunately.
            if(Math.abs(d - c.parentDist) - c.maxDist <= range) {
              open.add(c);
            }
          }
        }
        else { // Leaf node
          // Consider routing object, too:
          if(d <= range) {
            result.add(d, tmp); // First element is a candidate now
          }
        }
        // Skip routing object.
        final DoubleDBIDListIter it = cur.singletons.iter().advance();
        // For remaining singletons, compute the distances:
        while(it.valid()) {
          if(Math.abs(d - it.doubleValue()) <= range) {
            final double d2 = queryDistance(it);
            if(d2 <= range) {
              result.add(d2, it);
            }
          }
          it.advance();
        }
      }
      return result;
    }
  }

  /**
   * Range query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeRangeObjectSearcher extends CoverTreeRangeSearcher implements RangeSearcher<O> {
    /**
     * Query object.
     */
    private O query;

    @Override
    public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      return doSearch(range, result);
    }

    @Override
    protected double queryDistance(DBIDRef it) {
      return distance(query, it);
    }
  }

  /**
   * Range query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeRangeDBIDSearcher extends CoverTreeRangeSearcher implements RangeSearcher<DBIDRef> {
    /**
     * Query reference.
     */
    private DBIDRef query;

    @Override
    public ModifiableDoubleDBIDList getRange(DBIDRef query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      return doSearch(range, result);
    }

    @Override
    protected double queryDistance(DBIDRef it) {
      return distance(query, it);
    }
  }

  /**
   * KNN Query class.
   *
   * @author Erich Schubert
   */
  public abstract class CoverTreeKNNSearcher {
    /**
     * Priority queue of candidates.
     */
    private DoubleObjectMinHeap<Node> pq = new DoubleObjectMinHeap<>();

    /**
     * Temporary storage.
     */
    private DBIDVar tmp = DBIDUtil.newVar();

    /**
     * Do the main search
     *
     * @param k Number of neighbors to collect
     * @return results
     */
    protected KNNList doSearch(int k) {
      KNNHeap knnList = DBIDUtil.newHeap(k);
      double d_k = Double.POSITIVE_INFINITY;
      pq.clear();
      pq.add(queryDistance(root.singletons.iter()) - root.maxDist, root);

      // search in tree
      while(!pq.isEmpty()) {
        final Node cur = pq.peekValue();
        final double prio = pq.peekKey(); // Minimum distance to cover
        pq.poll(); // Remove

        if(knnList.size() >= k && prio > d_k) {
          continue;
        }
        final double d = prio + cur.maxDist; // Restore distance to center.

        final DoubleDBIDListIter it = cur.singletons.iter();
        if(!cur.children.isEmpty()) { // Inner node:
          for(Node c : cur.children) {
            // This only seems to reduce the number of distance computations
            // marginally, unfortunately.
            if(Math.abs(d - c.parentDist) - c.maxDist <= d_k) {
              // Reuse distance if the previous routing object is the same:
              double newprio = (DBIDUtil.equal(c.singletons.assignVar(0, tmp), it) //
                  ? d : queryDistance(tmp)) //
                  - c.maxDist; // Minimum distance
              if(newprio <= d_k) {
                pq.add(newprio, c);
              }
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
          if(Math.abs(d - it.doubleValue()) <= d_k) {
            final double d2 = queryDistance(it);
            if(d2 <= d_k) {
              d_k = knnList.insert(d2, it);
            }
          }
          it.advance();
        }
      }
      return knnList.toKNNList();
    }

    /**
     * Compute distance to query object.
     *
     * @param it Candidate
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef it);
  }

  /**
   * KNN Query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeKNNObjectSearcher extends CoverTreeKNNSearcher implements KNNSearcher<O> {
    /**
     * Query object.
     */
    private O query;

    @Override
    public KNNList getKNN(O obj, int k) {
      this.query = obj;
      return doSearch(k);
    }

    @Override
    protected double queryDistance(DBIDRef it) {
      return distance(query, it);
    }
  }

  /**
   * KNN Query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeKNNDBIDSearcher extends CoverTreeKNNSearcher implements KNNSearcher<DBIDRef> {
    /**
     * Query reference.
     */
    private DBIDRef query;

    @Override
    public KNNList getKNN(DBIDRef query, int k) {
      this.query = query;
      return doSearch(k);
    }

    @Override
    protected double queryDistance(DBIDRef it) {
      return distance(query, it);
    }
  }

  /**
   * Priority query class.
   *
   * @author Erich Schubert
   * 
   * @param <Q> query type
   */
  public abstract class CoverTreePrioritySearcher<Q> implements PrioritySearcher<Q> {
    /**
     * Stopping distance threshold.
     */
    double threshold = Double.POSITIVE_INFINITY;

    /**
     * Temporary storage.
     */
    private DBIDVar tmp = DBIDUtil.newVar();

    /**
     * Priority queue
     */
    private DoubleObjectMinHeap<Node> pq = new DoubleObjectMinHeap<>();

    /**
     * Candidates
     */
    private DoubleDBIDListIter candidates = DoubleDBIDListIter.EMPTY;

    /**
     * Distance to routing object.
     */
    private double routingDist;

    /**
     * Current lower bound.
     */
    private double lb;

    /**
     * Constructor.
     */
    public CoverTreePrioritySearcher() {
      super();
    }

    /**
     * Compute distance to query object.
     *
     * @param it Candidate
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef it);

    /**
     * Start the search.
     *
     * @return this.
     */
    protected PrioritySearcher<Q> doSearch() {
      this.threshold = Double.POSITIVE_INFINITY;
      this.candidates = DoubleDBIDListIter.EMPTY;
      pq.clear();
      pq.add(queryDistance(root.singletons.iter()) - root.maxDist, root);
      lb = 0.;
      return advance(); // Find first
    }

    @Override
    public PrioritySearcher<Q> decreaseCutoff(double threshold) {
      assert threshold <= this.threshold;
      this.threshold = threshold;
      return this;
    }

    @Override
    public double allLowerBound() {
      return lb;
    }

    @Override
    public boolean valid() {
      return candidates.valid();
    }

    @Override
    public PrioritySearcher<Q> advance() {
      // Advance the main iterator, if defined:
      if(candidates.valid()) {
        candidates.advance();
      }
      // First try the singletons
      // These aren't the best candidates usually, but we don't want to have to
      // manage them and their bounds in the heap. If we do this locally, we get
      // upper and lower bounds easily.
      do {
        while(candidates.valid()) {
          // Pruning with lower bound:
          if(Math.abs(routingDist - candidates.doubleValue()) <= threshold) {
            return this;
          }
          candidates.advance(); // Skip
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
      lb = prio > lb ? prio : lb;
      routingDist = prio + cur.maxDist; // Restore distance to center.
      candidates = cur.singletons.iter(); // Routing object initially
      pq.poll(); // Remove

      // Add child nodes to priority queue:
      for(Node c : cur.children) {
        // This pruning rule very rarely works, unfortunately
        if(Math.abs(routingDist - c.parentDist) - c.maxDist <= threshold) {
          // Reuse distance if the previous routing object is the same:
          double newprio = (DBIDUtil.equal(c.singletons.assignVar(0, tmp), candidates) //
              ? routingDist : queryDistance(tmp)) //
              - c.maxDist; // Minimum distance
          if(newprio <= threshold) {
            pq.add(newprio, c);
          }
        }
      }
      if(!cur.children.isEmpty()) {
        candidates.advance(); // Skip routing object (also in children)
      }
      return true;
    }

    @Override
    public double getApproximateDistance() {
      return MathUtil.max(candidates.doubleValue(), routingDist);
    }

    @Override
    public double getApproximateAccuracy() {
      return candidates.getOffset() == 0 ? 0. : MathUtil.min(candidates.doubleValue(), routingDist);
    }

    @Override
    public double getLowerBound() {
      return candidates.getOffset() == 0 ? routingDist : MathUtil.max(lb, Math.abs(candidates.doubleValue() - routingDist));
    }

    @Override
    public double getUpperBound() {
      return candidates.getOffset() == 0 ? routingDist : candidates.doubleValue() + routingDist;
    }

    @Override
    public double computeExactDistance() {
      return candidates.getOffset() == 0 || candidates.doubleValue() == 0 ? routingDist : queryDistance(candidates);
    }

    @Override
    public int internalGetIndex() {
      return candidates.internalGetIndex();
    }
  }

  /**
   * Priority query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreePriorityObjectSearcher extends CoverTreePrioritySearcher<O> {
    /**
     * Query object
     */
    private O query;

    @Override
    public PrioritySearcher<O> search(O query) {
      this.query = query;
      doSearch();
      return this;
    }

    @Override
    protected double queryDistance(DBIDRef it) {
      return distance(query, it);
    }
  }

  /**
   * Priority query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreePriorityDBIDSearcher extends CoverTreePrioritySearcher<DBIDRef> {
    /**
     * Query object
     */
    private DBIDRef query;

    @Override
    public PrioritySearcher<DBIDRef> search(DBIDRef query) {
      this.query = query;
      doSearch();
      return this;
    }

    @Override
    protected double queryDistance(DBIDRef it) {
      return distance(query, it);
    }
  }

  /**
   * Index factory.
   *
   * @author Erich Schubert
   *
   * @has - - - CoverTree
   *
   * @param <O> Object type
   */
  public static class Factory<O> extends AbstractCoverTree.Factory<O> {
    /**
     * Constructor.
     *
     * @param distance Distance function
     * @param expansion Expansion rate
     * @param truncate Truncate branches with less than this number of instances
     */
    public Factory(Distance<? super O> distance, double expansion, int truncate) {
      super(distance, expansion, truncate);
    }

    @Override
    public CoverTree<O> instantiate(Relation<O> relation) {
      return new CoverTree<>(relation, distance, expansion, truncate);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends AbstractCoverTree.Factory.Par<O> {
      @Override
      public CoverTree.Factory<O> make() {
        return new CoverTree.Factory<>(distance, expansion, truncate);
      }
    }
  }
}
