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
package de.lmu.ifi.dbs.elki.index.tree.metrical.covertree;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleObjectMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Cover tree data structure (in-memory). This is a <i>metrical</i> data
 * structure that is similar to the M-tree, but not as balanced and
 * disk-oriented. However, by not having these requirements it does not require
 * the expensive splitting procedures of M-tree.
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
 * @has - - - CoverTreeRangeQuery
 * @has - - - CoverTreeKNNQuery
 */
@Reference(authors = "A. Beygelzimer, S. Kakade, J. Langford", //
    title = "Cover trees for nearest neighbor", //
    booktitle = "In Proc. 23rd Int. Conf. Machine Learning (ICML 2006)", //
    url = "https://doi.org/10.1145/1143844.1143857", //
    bibkey = "DBLP:conf/icml/BeygelzimerKL06")
public class CoverTree<O> extends AbstractCoverTree<O> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * Class logger.
   */
  static final Logging LOG = Logging.getLogger(CoverTree.class);

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
  public CoverTree(Relation<O> relation, DistanceFunction<? super O> distanceFunction, double expansion, int truncate) {
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
    ArrayList<Node> children;

    /**
     * Expansion scale.
     */
    // int scale = SCALE_LEAF;

    /**
     * Constructor.
     *
     * @param r Object.
     * @param maxDist Maximum distance to any descendant.
     * @param parentDist Distance from parent.
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
     * @param r Object.
     * @param maxDist Maximum distance to any descendant.
     * @param parentDist Distance from parent.
     * @param singletons Singletons.
     */
    public Node(DBIDRef r, double maxDist, double parentDist, DoubleDBIDList singletons) {
      assert (!singletons.contains(r));
      this.singletons = DBIDUtil.newDistanceDBIDList(singletons.size() + 1);
      this.singletons.add(0., r);
      for(DoubleDBIDListIter it = singletons.iter(); it.valid(); it.advance()) {
        this.singletons.add(it.doubleValue(), it);
      }
      this.children = null;
      this.maxDist = maxDist;
      this.parentDist = parentDist;
    }

    /**
     * True, if the node is a leaf.
     *
     * @return {@code true}, if this is a leaf node.
     */
    public boolean isLeaf() {
      return children == null || children.isEmpty();
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
    root = bulkConstruct(first, Integer.MAX_VALUE, 0., candidates);
  }

  /**
   * Bulk-load the cover tree.
   *
   * This bulk-load is slightly simpler than the one used in the original
   * cover-tree source: We do not look back into the "far" set of candidates.
   *
   * @param cur Current routing object
   * @param maxScale Maximum scale
   * @param elems Candidates
   * @return Root node of subtree
   */
  protected Node bulkConstruct(DBIDRef cur, int maxScale, double parentDist, ModifiableDoubleDBIDList elems) {
    assert (!elems.contains(cur));
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
    if(candidates.size() == 0) {
      LOG.warning("Scale not chosen appropriately? " + max + " " + scaleToDist(scale));
      return bulkConstruct(cur, nextScale, parentDist, elems);
    }
    // We will have at least one other child, so build the parent:
    Node node = new Node(cur, max, parentDist);
    // Routing element now is a singleton:
    final boolean curSingleton = elems.size() == 0;
    if(!curSingleton) {
      // Add node for the routing object:
      node.children.add(bulkConstruct(cur, nextScale, 0, elems));
    }
    final double fmax = scaleToDist(nextScale);
    // Build additional cover nodes:
    for(DoubleDBIDListIter it = candidates.iter(); it.valid();) {
      assert (it.getOffset() == 0);
      DBID t = DBIDUtil.deref(it);
      elems.clear(); // Recycle.
      collectByCover(it, candidates, fmax, elems);
      assert (DBIDUtil.equal(t, it)) : "First element in candidates must not change!";
      if(elems.size() == 0) { // Singleton
        node.singletons.add(it.doubleValue(), it);
      }
      else {
        // Build a full child node:
        node.children.add(bulkConstruct(it, nextScale, it.doubleValue(), elems));
      }
      candidates.removeSwap(0);
    }
    assert (candidates.size() == 0);
    // Routing object is not yet handled:
    if(curSingleton) {
      if(node.isLeaf()) {
        node.children = null; // First in leaf is enough.
      }
      else {
        node.singletons.add(parentDist, cur); // Add as regular singleton.
      }
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
    counts[4] += cur.singletons.size() - (cur.children == null ? 0 : 1);
    if(cur.children != null) {
      ++depth;
      for(Node chi : cur.children) {
        checkCoverTree(chi, counts, depth);
      }
      assert (!cur.children.isEmpty()) : "Empty childs list.";
    }
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    DistanceQuery<O> dq = distanceFunction.instantiate(relation);
    return new CoverTreeRangeQuery(dq);
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      return null;
    }
    DistanceQuery<O> dq = distanceFunction.instantiate(relation);
    return new CoverTreeKNNQuery(dq);
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
  public class CoverTreeRangeQuery extends AbstractDistanceRangeQuery<O> implements RangeQuery<O> {
    /**
     * Constructor.
     *
     * @param distanceQuery Distance query
     */
    public CoverTreeRangeQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList ret) {
      ArrayList<Node> open = new ArrayList<Node>(); // LIFO stack
      open.add(root);
      while(!open.isEmpty()) {
        final Node cur = open.remove(open.size() - 1); // pop()
        final DoubleDBIDListIter it = cur.singletons.iter();
        final double d = distance(obj, it);
        // Covered area not in range (metric assumption!):
        if(d - cur.maxDist > range) {
          continue;
        }
        if(!cur.isLeaf()) { // Inner node:
          for(Node c : cur.children) {
            // This only seems to reduce the number of distance computations
            // marginally, unfortunately.
            if(d - c.maxDist - c.parentDist <= range) {
              open.add(c);
            }
          }
        }
        else { // Leaf node
          // Consider routing object, too:
          if(d <= range) {
            ret.add(d, it); // First element is a candidate now
          }
        }
        it.advance(); // Skip routing object.
        // For remaining singletons, compute the distances:
        while(it.valid()) {
          if(d - it.doubleValue() <= range) {
            final double d2 = distance(obj, it);
            if(d2 <= range) {
              ret.add(d2, it);
            }
          }
          it.advance();
        }
      }
    }
  }

  /**
   * KNN Query class.
   *
   * @author Erich Schubert
   */
  public class CoverTreeKNNQuery extends AbstractDistanceKNNQuery<O> implements KNNQuery<O> {
    /**
     * Constructor.
     *
     * @param distanceQuery Distance
     */
    public CoverTreeKNNQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
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

        final DoubleDBIDListIter it = cur.singletons.iter();
        if(!cur.isLeaf()) { // Inner node:
          for(Node c : cur.children) {
            // This only seems to reduce the number of distance computations
            // marginally, unfortunately.
            if(d - c.maxDist - c.parentDist <= d_k) {
              final DoubleDBIDListIter f = c.singletons.iter();
              final double dist = DBIDUtil.equal(f, it) ? d : distance(obj, f);
              final double newprio = dist - c.maxDist; // Minimum distance
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
          if(d - it.doubleValue() <= d_k) {
            final double d2 = distance(obj, it);
            if(d2 <= d_k) {
              d_k = knnList.insert(d2, it);
            }
          }
          it.advance();
        }
      }
      return knnList.toKNNList();
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
     * @param distanceFunction Distance function
     * @param expansion Expansion rate
     * @param truncate Truncate branches with less than this number of
     *        instances.
     */
    public Factory(DistanceFunction<? super O> distanceFunction, double expansion, int truncate) {
      super(distanceFunction, expansion, truncate);
    }

    @Override
    public CoverTree<O> instantiate(Relation<O> relation) {
      return new CoverTree<O>(relation, distanceFunction, expansion, truncate);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer<O> extends AbstractCoverTree.Factory.Parameterizer<O> {
      @Override
      protected CoverTree.Factory<O> makeInstance() {
        return new CoverTree.Factory<>(distanceFunction, expansion, truncate);
      }
    }
  }
}
