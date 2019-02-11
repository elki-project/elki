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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import java.util.ArrayList;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Partial implementation of the CFTree as used by BIRCH.
 * <p>
 * Important differences:
 * <ol>
 * <li>Leaf nodes and directory nodes have the same capacity</li>
 * <li>Condensing and memory limits are not implemented</li>
 * <li>Merging refinement (merge-resplit) is not implemented</li>
 * </ol>
 * Because we want to be able to track the cluster assignments of all data
 * points easily, we need to store the point IDs, and it is not possible to
 * implement the originally proposed page size management at the same time.
 * <p>
 * Condensing and merging refinement are possible, and improvements to this code
 * are welcome - please send a pull request!
 * <p>
 * References:
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: An Efficient Data Clustering Method for Very Large Databases
 * Proc. 1996 ACM SIGMOD International Conference on Management of Data
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: A New Data Clustering Algorithm and Its Applications<br>
 * Data Min. Knowl. Discovery
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - ClusteringFeature
 * @has - - - LeafIterator
 */
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/233269.233324", //
    bibkey = "DBLP:conf/sigmod/ZhangRL96")
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: A New Data Clustering Algorithm and Its Applications", //
    booktitle = "Data Min. Knowl. Discovery", //
    url = "https://doi.org/10.1023/A:1009783824328", //
    bibkey = "DBLP:journals/datamine/ZhangRL97")
public class CFTree {
  /**
   * Class logger.
   */
  public static final Logging LOG = Logging.getLogger(CFTree.class);

  /**
   * Distance function to use.
   */
  BIRCHDistance distance;

  /**
   * Criterion for absorbing points.
   */
  BIRCHAbsorptionCriterion absorption;

  /**
   * Squared maximum radius threshold of a clusterin feature.
   */
  double thresholdsq;

  /**
   * Capacity of a node.
   */
  int capacity;

  /**
   * Current root node.
   */
  TreeNode root = null;

  /**
   * Leaf node counter.
   */
  int leaves;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param absorption Absorption criterion
   * @param threshold Threshold
   * @param capacity Capacity
   */
  public CFTree(BIRCHDistance distance, BIRCHAbsorptionCriterion absorption, double threshold, int capacity) {
    super();
    this.distance = distance;
    this.absorption = absorption;
    this.thresholdsq = threshold * threshold;
    this.capacity = capacity;
  }

  /**
   * Insert a data point into the tree.
   *
   * @param nv Object data
   */
  public void insert(NumberVector nv) {
    final int dim = nv.getDimensionality();
    // No root created yet:
    if(root == null) {
      ClusteringFeature leaf = new ClusteringFeature(dim);
      leaf.addToStatistics(nv);
      root = new TreeNode(dim, capacity);
      root.children[0] = leaf;
      root.addToStatistics(nv);
      ++leaves;
      return;
    }
    TreeNode other = insert(root, nv);
    // Handle root overflow:
    if(other != null) {
      TreeNode newnode = new TreeNode(dim, capacity);
      newnode.addToStatistics(newnode.children[0] = root);
      newnode.addToStatistics(newnode.children[1] = other);
      root = newnode;
    }
  }

  /**
   * Rebuild the CFTree to condense it to approximately half the size.
   */
  protected void rebuildTree() {
    final int dim = root.getDimensionality();
    double t = estimateThreshold(root) / leaves;
    t *= t;
    // Never decrease the threshold.
    thresholdsq = t > thresholdsq ? t : thresholdsq;
    LOG.debug("New squared threshold: " + thresholdsq);

    LeafIterator iter = new LeafIterator(root); // Will keep the old root.
    assert (iter.valid());
    ClusteringFeature first = iter.get();

    leaves = 0;
    // Make a new root node:
    root = new TreeNode(dim, capacity);
    root.children[0] = first;
    root.addToStatistics(first);
    ++leaves;
    for(iter.advance(); iter.valid(); iter.advance()) {
      TreeNode other = insert(root, iter.get());
      // Handle root overflow:
      if(other != null) {
        TreeNode newnode = new TreeNode(dim, capacity);
        newnode.addToStatistics(newnode.children[0] = root);
        newnode.addToStatistics(newnode.children[1] = other);
        root = newnode;
      }
    }
  }

  private double estimateThreshold(TreeNode current) {
    ClusteringFeature[] children = current.children;
    double total = 0.;
    if(!(children[0] instanceof TreeNode)) {
      if(children[1] == null) {
        return 0.;
      }
      double[] best = new double[children.length]; // Cache.
      Arrays.fill(best, Double.POSITIVE_INFINITY);
      int[] besti = new int[children.length];
      for(int i = 0; i < children.length; i++) {
        ClusteringFeature ci = children[i];
        if(ci == null) {
          break;
        }
        double bi = best[i];
        int bestii = besti[i];
        for(int j = i + 1; j < children.length; j++) {
          if(children[j] == null) {
            break;
          }
          // double dist = absorption.squaredCriterion(ci, children[j]);
          double dist = distance.squaredDistance(ci, children[j]);
          if(dist < bi) {
            bi = dist;
            bestii = j;
          }
          if(dist < best[j]) {
            best[j] = dist;
            besti[j] = i;
          }
        }
        double t = absorption.squaredCriterion(ci, children[bestii]);
        total += t > 0 ? Math.sqrt(t) : 0;
      }
    }
    else {
      assert (children[0] instanceof TreeNode) : "Node is neither child nor inner?";
      for(int i = 0; i < children.length; i++) {
        if(children[i] == null) {
          break;
        }
        total += estimateThreshold((TreeNode) children[i]);
      }
    }
    return total;
  }

  /**
   * Recursive insertion.
   *
   * @param node Current node
   * @param nv Object data
   * @return New sibling, if the node was split.
   */
  private TreeNode insert(TreeNode node, NumberVector nv) {
    // Find closest child:
    ClusteringFeature[] cfs = node.children;
    assert (cfs[0] != null) : "Unexpected empty node!";

    // Find the best child:
    ClusteringFeature best = cfs[0];
    double bestd = distance.squaredDistance(nv, best);
    for(int i = 1; i < cfs.length; i++) {
      ClusteringFeature cf = cfs[i];
      if(cf == null) {
        break;
      }
      double d2 = distance.squaredDistance(nv, cf);
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    // Leaf node:
    if(!(best instanceof TreeNode)) {
      // Threshold constraint satisfied?
      if(absorption.squaredCriterion(best, nv) <= thresholdsq) {
        best.addToStatistics(nv);
        node.addToStatistics(nv);
        return null;
      }
      best = new ClusteringFeature(nv.getDimensionality());
      best.addToStatistics(nv);
      ++leaves;
      if(add(node.children, best)) {
        node.addToStatistics(nv); // Update statistics
        return null;
      }
      return split(node, best);
    }
    assert (best instanceof TreeNode) : "Node is neither child nor inner?";
    TreeNode newchild = insert((TreeNode) best, nv);
    if(newchild == null || add(node.children, newchild)) {
      node.addToStatistics(nv); // Update statistics
      return null;
    }
    return split(node, newchild);
  }

  /**
   * Find the leaf of a cluster, to get the final cluster assignment.
   * <p>
   * In contrast to {@link #insert}, this does not modify the tree.
   *
   * @param nv Object data
   * @return Leaf this vector should be assigned to
   */
  public ClusteringFeature findLeaf(NumberVector nv) {
    if(root == null) {
      throw new IllegalStateException("CFTree not yet built.");
    }
    return findLeaf(root, nv);
  }

  /**
   * Find the leaf of a cluster, to get the final cluster assignment.
   * <p>
   * In contrast to {@link #insert}, this does not modify the tree.
   * <p>
   * TODO: allow "outliers"?
   *
   * @param node Current node
   * @param nv Object data
   * @return Leaf this vector should be assigned to
   */
  private ClusteringFeature findLeaf(TreeNode node, NumberVector nv) {
    // Find closest child:
    ClusteringFeature[] cfs = node.children;
    assert (cfs[0] != null) : "Unexpected empty node!";

    // Find the best child:
    ClusteringFeature best = cfs[0];
    double bestd = distance.squaredDistance(nv, best);
    for(int i = 1; i < cfs.length; i++) {
      ClusteringFeature cf = cfs[i];
      if(cf == null) {
        break;
      }
      double d2 = distance.squaredDistance(nv, cf);
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    return (best instanceof TreeNode) ? findLeaf((TreeNode) best, nv) : best;
  }

  /**
   * Split an overfull node.
   *
   * @param node Node to split
   * @param newchild Additional child
   * @return New sibling of {@code node}
   */
  private TreeNode split(TreeNode node, ClusteringFeature newchild) {
    final int capacity = node.children.length;
    assert (node.children[capacity - 1] != null) : "Node to split is not empty!";
    TreeNode newn = new TreeNode(node.getDimensionality(), capacity);
    final int size = capacity + 1;
    // Find farthest pair:
    int m1 = -1, m2 = -1;
    double maxd = Double.NEGATIVE_INFINITY;
    double[][] dists = new double[size][size];
    for(int i = 0; i < capacity; i++) {
      ClusteringFeature ci = node.children[i];
      for(int j = i + 1; j < capacity; j++) {
        double d = dists[i][j] = dists[j][i] = distance.squaredDistance(ci, node.children[j]);
        if(d > maxd) {
          maxd = d;
          m1 = i;
          m2 = j;
        }
      }
      double d = dists[i][capacity] = dists[capacity][i] = distance.squaredDistance(ci, newchild);
      if(d > maxd) {
        maxd = d;
        m1 = i;
        m2 = capacity;
      }
    }
    // Reset node statistics:
    node.resetStatistics();
    newn.resetStatistics();
    // Redistribute entries:
    int si = 0, sj = 0; // Output positions.
    double[] d1s = dists[m1], d2s = dists[m2];
    for(int i = 0; i < capacity; i++) {
      double d1 = d1s[i], d2 = d2s[i];
      if(i == m1 || i != m2 && (d1 < d2 || (d1 == d2 && si <= sj))) {
        node.addToStatistics(node.children[si++] = node.children[i]);
      }
      else {
        newn.addToStatistics(newn.children[sj++] = node.children[i]);
      }
    }
    {
      double d1 = d1s[capacity], d2 = d2s[capacity];
      if(capacity != m2 && (d1 < d2 || (d1 == d2 && si <= sj))) {
        node.addToStatistics(node.children[si++] = newchild);
      }
      else {
        newn.addToStatistics(newn.children[sj++] = newchild);
      }
    }
    for(int j = si; j < capacity; j++) {
      node.children[j] = null;
    }
    for(int j = sj; j < capacity; j++) {
      assert (newn.children[j] == null);
    }
    return newn;
  }

  /**
   * Recursive insertion.
   *
   * @param node Current node
   * @param nleaf Leaf entry to add.
   * @return New sibling, if the node was split.
   */
  private TreeNode insert(TreeNode node, ClusteringFeature nleaf) {
    // Find closest child:
    ClusteringFeature[] cfs = node.children;
    assert (cfs[0] != null) : "Unexpected empty node!";

    // Find the best child:
    ClusteringFeature best = cfs[0];
    double bestd = distance.squaredDistance(nleaf, best);
    for(int i = 1; i < cfs.length; i++) {
      ClusteringFeature cf = cfs[i];
      if(cf == null) {
        break;
      }
      double d2 = distance.squaredDistance(nleaf, cf);
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    assert (best != nleaf);
    if(!(best instanceof TreeNode)) {
      // Threshold constraint satisfied?
      if(absorption.squaredCriterion(best, nleaf) <= thresholdsq) {
        best.addToStatistics(nleaf);
        node.addToStatistics(nleaf);
        return null;
      }
      ++leaves; // We have to add this entry
      if(add(node.children, nleaf)) {
        node.addToStatistics(nleaf); // Update statistics
        return null;
      }
      return split(node, nleaf);
    }
    assert (best instanceof TreeNode) : "Node is neither child nor inner?";
    TreeNode newchild = insert((TreeNode) best, nleaf);
    if(newchild == null || add(node.children, newchild)) {
      node.addToStatistics(nleaf); // Update statistics
      return null;
    }
    return split(node, newchild);
  }

  /**
   * Add a node to the first unused slot.
   *
   * @param children Children list
   * @param child Child to add
   * @return {@code false} if node is full
   */
  private boolean add(ClusteringFeature[] children, ClusteringFeature child) {
    for(int i = 0; i < children.length; i++) {
      if(children[i] == null) {
        children[i] = child;
        return true;
      }
    }
    return false;
  }

  /**
   * Get an iterator over the leaf nodes.
   *
   * @return Leaf node iterator.
   */
  public LeafIterator leafIterator() {
    return new LeafIterator(root);
  }

  /**
   * Iterator over leaf nodes.
   *
   * @author Erich Schubert
   */
  public static class LeafIterator implements Iter {
    /**
     * Queue of open ends.
     */
    private ArrayList<ClusteringFeature> queue;

    /**
     * Current leaf entry.
     */
    private ClusteringFeature current;

    /**
     * Constructor.
     * 
     * @param root Root node
     */
    private LeafIterator(TreeNode root) {
      queue = new ArrayList<>();
      queue.add(root);
      advance();
    }

    @Override
    public boolean valid() {
      return current != null;
    }

    /**
     * Get the current leaf.
     *
     * @return Current leaf (if valid, {@code null} otherwise).
     */
    public ClusteringFeature get() {
      return current;
    }

    @Override
    public Iter advance() {
      current = null;
      while(queue.size() > 0) {
        // Pop last element
        ClusteringFeature f = queue.remove(queue.size() - 1);
        if(!(f instanceof TreeNode)) { // lead
          current = f;
          break;
        }
        for(ClusteringFeature c : ((TreeNode) f).children) {
          if(c == null) {
            break;
          }
          queue.add(c);
        }
      }
      return this;
    }
  }

  /**
   * Inner node.
   *
   * @hidden
   *
   * @author Erich Schubert
   */
  public static class TreeNode extends ClusteringFeature {
    ClusteringFeature[] children;

    public TreeNode(int dim, int capacity) {
      super(dim);
      children = new ClusteringFeature[capacity];
    }
  }

  /**
   * Utility function for debugging.
   *
   * @param buf Output buffer
   * @param n Current node
   * @param d Depth
   * @return Output buffer
   */
  protected StringBuilder printDebug(StringBuilder buf, ClusteringFeature n, int d) {
    FormatUtil.appendSpace(buf, d).append(n.n);
    for(int i = 0; i < n.getDimensionality(); i++) {
      buf.append(' ').append(n.centroid(i));
    }
    buf.append(" - ").append(n.n).append('\n');
    if(n instanceof TreeNode) {
      ClusteringFeature[] children = ((TreeNode) n).children;
      for(int i = 0; i < children.length; i++) {
        ClusteringFeature c = children[i];
        if(c != null) {
          printDebug(buf, c, d + 1);
        }
      }
    }
    return buf;
  }

  /**
   * CF-Tree Factory.
   *
   * @author Erich Schubert
   */
  public static class Factory {
    /**
     * BIRCH distance function to use
     */
    BIRCHDistance distance;

    /**
     * Criterion for absorbing points.
     */
    BIRCHAbsorptionCriterion absorption;

    /**
     * Cluster merge threshold.
     */
    double threshold;

    /**
     * Maximum branching factor of CFTree.
     */
    int branchingFactor;

    /**
     * Maximum number of leaves (absolute or relative)
     */
    double maxleaves;

    /**
     * Constructor.
     *
     * @param distance Distance to use
     * @param absorption Absorption criterion (diameter, distance).
     * @param threshold Distance threshold
     * @param branchingFactor Maximum branching factor.
     * @param maxleaves Maximum number of leaves
     */
    public Factory(BIRCHDistance distance, BIRCHAbsorptionCriterion absorption, double threshold, int branchingFactor, double maxleaves) {
      this.distance = distance;
      this.absorption = absorption;
      this.threshold = threshold;
      this.branchingFactor = branchingFactor;
      this.maxleaves = maxleaves;
    }

    /**
     * Make a new tree.
     *
     * @param ids DBIDs to insert
     * @param relation Data relation
     * @return New tree
     */
    public CFTree newTree(DBIDs ids, Relation<? extends NumberVector> relation) {
      CFTree tree = new CFTree(distance, absorption, threshold, branchingFactor);
      final double max = maxleaves <= 1 ? maxleaves * ids.size() : maxleaves;
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Building tree", relation.size(), LOG) : null;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        tree.insert(relation.get(it));
        if(tree.leaves > max) {
          if(LOG.isVerbose()) {
            LOG.verbose("Compacting CF-tree.");
          }
          tree.rebuildTree();
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      return tree;
    }

    /**
     * Parameterization class for CFTrees.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * Distance function parameter.
       */
      public static final OptionID DISTANCE_ID = new OptionID("cftree.distance", "Distance function to use for node assignment.");

      /**
       * Absorption parameter.
       */
      public static final OptionID ABSORPTION_ID = new OptionID("cftree.absorption", "Absorption criterion to use.");

      /**
       * Distance threshold.
       */
      public static final OptionID THRESHOLD_ID = new OptionID("cftree.threshold", "Threshold for adding points to existing nodes in the CF-Tree.");

      /**
       * Branching factor.
       */
      public static final OptionID BRANCHING_ID = new OptionID("cftree.branching", "Maximum branching factor of the CF-Tree");

      /**
       * Maximum number of leaves.
       */
      public static final OptionID MAXLEAVES_ID = new OptionID("cftree.maxleaves", "Maximum number of leaves (if less than 1, the values is assumed to be relative)");

      /**
       * BIRCH distance function to use
       */
      BIRCHDistance distance;

      /**
       * Criterion for absorbing points.
       */
      BIRCHAbsorptionCriterion absorption;

      /**
       * Cluster merge threshold.
       */
      double threshold = 0.;

      /**
       * Maximum branching factor of CFTree.
       */
      int branchingFactor;

      /**
       * Maximum number of leaves (absolute or relative)
       */
      double maxleaves;

      @Override
      protected void makeOptions(Parameterization config) {
        ObjectParameter<BIRCHDistance> distanceP = new ObjectParameter<>(DISTANCE_ID, BIRCHDistance.class, VarianceIncreaseDistance.class);
        if(config.grab(distanceP)) {
          distance = distanceP.instantiateClass(config);
        }

        ObjectParameter<BIRCHAbsorptionCriterion> absorptionP = new ObjectParameter<>(ABSORPTION_ID, BIRCHAbsorptionCriterion.class, DiameterCriterion.class);
        if(config.grab(absorptionP)) {
          absorption = absorptionP.instantiateClass(config);
        }

        DoubleParameter thresholdP = new DoubleParameter(THRESHOLD_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
            .setOptional(true);
        if(config.grab(thresholdP)) {
          threshold = thresholdP.doubleValue();
        }

        IntParameter branchingP = new IntParameter(BRANCHING_ID) //
            .addConstraint(new GreaterEqualConstraint(2)) //
            .setDefaultValue(64);
        if(config.grab(branchingP)) {
          branchingFactor = branchingP.intValue();
        }

        DoubleParameter maxleavesP = new DoubleParameter(MAXLEAVES_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .setDefaultValue(0.05);
        if(config.grab(maxleavesP)) {
          maxleaves = maxleavesP.doubleValue();
        }
      }

      @Override
      protected CFTree.Factory makeInstance() {
        return new CFTree.Factory(distance, absorption, threshold, branchingFactor, maxleaves);
      }
    }
  }
}
