/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.hierarchical.betula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import elki.clustering.hierarchical.betula.vvi.VVIModel;
import elki.data.NumberVector;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.MathUtil;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.datastructures.iterator.Iter;
import elki.utilities.documentation.Reference;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @author Andreas Lang
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
public class CFTree<L extends CFInterface> {
  /**
   * Class logger.
   */
  public static final Logging LOG = Logging.getLogger(CFTree.class);

  /**
   * Threshold update strategy.
   * 
   * @author Andreas Lang
   */
  public enum Threshold {
    /**
     * Split halfway between minimum and maximum. This is also sometimes called
     * the "sliding midpoint split", because we use the minimal bounding boxes,
     * not the previous cell.
     */
    MEAN,
    /**
     * Split using the median.
     */
    MEDIAN
  }

  /**
   * Threshold heuristic strategy.
   */
  protected Threshold tCriterium;

  /**
   * Squared maximum radius threshold of a clustering feature.
   */
  double thresholdsq;

  /**
   * Capacity of a node.
   */
  int capacity;

  /**
   * Current root node.
   */
  CFNode<L> root = null;

  /**
   * Leaf node counter.
   */
  int leaves;

  /**
   * data relation
   */
  Relation<? extends NumberVector> relation;

  /**
   * Stored leaf entry to dbid relation
   */
  protected Map<CFInterface, ArrayModifiableDBIDs> idmap = null;

  /**
   * Cluster feature model
   */
  CFModel<L> cfModel;

  /**
   * Cluster distance
   */
  CFDistance<L> dist;

  /**
   * Absorption criterion
   */
  CFDistance<L> abs;

  /**
   * Constructor.
   *
   * @param threshold Threshold
   * @param capacity Capacity
   */
  public CFTree(CFModel<L> cfModel, Relation<? extends NumberVector> relation, double threshold, int capacity, Threshold tCriterium) {
    super();
    this.relation = relation;
    this.thresholdsq = threshold * threshold;
    this.capacity = capacity;
    this.tCriterium = tCriterium;
    this.cfModel = cfModel;
    this.dist = cfModel.distance();
    this.abs = cfModel.absorption();
  }

  /**
   * Insert a data point into the tree.
   *
   * @param nv Object data
   */
  public void insert(DBIDIter dbid) {
    // No root created yet:
    if(root == null) {
      NumberVector nv = relation.get(dbid);
      final int dim = nv.getDimensionality();
      L leaf = cfModel.make(dim);
      if(idmap != null) {
        ArrayModifiableDBIDs list = DBIDUtil.newArray();
        list.add(dbid);
        idmap.put(leaf, list);
      }
      leaf.addToStatistics(nv);
      root = cfModel.treeNode(dim, capacity);
      root.setChild(0, leaf);
      root.addToStatistics(nv);
      ++leaves;
      return;
    }
    CFNode<L> other = insert(root, dbid);
    // Handle root overflow:
    if(other != null) {
      final int dim = other.getDimensionality();
      CFNode<L> newnode = cfModel.treeNode(dim, capacity);
      newnode.addToStatistics(0, root);
      newnode.addToStatistics(1, other);
      root = newnode;
    }
  }

  /**
   * Rebuild the CFTree to condense it to approximately half the size.
   */
  protected void rebuildTree() {
    final int dim = root.getDimensionality();
    int oldLeaves = leaves;
    ArrayList<L> cfs = new ArrayList<>(leaves);
    double[] thresholds = new double[leaves];
    estimateThreshold(root, cfs, thresholds);
    int[] order = MathUtil.sequence(0, leaves);
    // DoubleIntegerArrayQuickSort.sort(thresholds, order, leaves);

    double t = 0;
    if(tCriterium == Threshold.MEAN) {
      int n = 0;
      for(int i = 0; i < leaves; i++) {
        if(thresholds[i] < Double.POSITIVE_INFINITY) {
          t += Math.sqrt(thresholds[i]);
          n++;
        }
      }
      t = t / n;
      t *= t;
    }
    else if(tCriterium == Threshold.MEDIAN) {
      DoubleIntegerArrayQuickSort.sort(thresholds, order, leaves);
      int median = cfs.size() >>> 1;
      t = thresholds[median];
      while(t == Double.POSITIVE_INFINITY && median > 0) {
        t = thresholds[--median];
      }
    }
    else {
      throw new IllegalStateException("Unknown threshold heuristic.");
    }
    // Never decrease the threshold.
    thresholdsq = t > thresholdsq ? t : thresholdsq;
    LOG.debug("New squared threshold: " + thresholdsq);

    leaves = 0;
    // Make a new root node:
    root = cfModel.treeNode(dim, capacity);
    L first = cfs.get(order[cfs.size() - 1]);
    root.addToStatistics(0, first);
    ++leaves;
    for(int i = cfs.size() - 2; i >= 0; i--) {
      CFNode<L> other = insert(root, cfs.get(order[i]));
      // Handle root overflow:
      if(other != null) {
        CFNode<L> newnode = cfModel.treeNode(dim, capacity);
        newnode.addToStatistics(0, root);
        newnode.addToStatistics(1, other);
        root = newnode;
      }
    }
    // Can't guarantee compacting without sort but sort is to expensive.
    if(leaves > oldLeaves) {
      throw new IllegalStateException("Could not reduce the number of leaves when compacting tree");
    }
  }

  @SuppressWarnings("unchecked")
  private void estimateThreshold(CFNode<L> current, ArrayList<L> cfs, double[] thresholds) {
    int offset = cfs.size();
    if(!(current.getChild(0) instanceof CFNode)) {
      if(current.getChild(1) == null) {
        thresholds[offset++] = Double.POSITIVE_INFINITY;
        cfs.add(current.getChild(0));
        return;
      }
      double[] best = new double[capacity]; // Cache.
      Arrays.fill(best, Double.POSITIVE_INFINITY);
      int[] besti = new int[capacity];
      for(int i = 0; i < capacity; i++) {
        L ci = current.getChild(i);
        if(ci == null) {
          break;
        }
        double bi = best[i];
        int bestii = besti[i];
        for(int j = i + 1; j < capacity; j++) {
          if(current.getChild(j) == null) {
            break;
          }
          // double dist = absorption.squaredCriterion(ci, children[j]);
          double d = dist.squaredDistance(ci, current.getChild(j));
          if(d < bi) {
            bi = d;
            bestii = j;
          }
          if(d < best[j]) {
            best[j] = d;
            besti[j] = i;
          }
        }
        double t = abs.squaredDistance(ci, current.getChild(bestii));
        thresholds[offset++] = t;
        cfs.add(ci);
      }
    }
    else {
      assert (current.getChild(0) instanceof CFNode) : "Node is neither child nor inner?";
      for(int i = 0; i < capacity; i++) {
        if(current.getChild(i) == null) {
          break;
        }
        estimateThreshold((CFNode<L>) current.getChild(i), cfs, thresholds);
      }
    }
  }

  /**
   * Recursive insertion.
   *
   * @param node Current node
   * @param nv Object data
   * @return New sibling, if the node was split.
   */
  @SuppressWarnings("unchecked")
  private CFNode<L> insert(CFNode<L> node, DBIDIter dbid) {
    NumberVector nv = relation.get(dbid);
    // Find closest child:
    assert (node.getChild(0) != null) : "Unexpected empty node!";

    // Find the best child:
    L best = node.getChild(0);
    double bestd = dist.squaredDistance(nv, best);
    for(int i = 1; i < capacity; i++) {
      L cf = node.getChild(i);
      if(cf == null) {
        break;
      }
      double d2 = dist.squaredDistance(nv, cf);
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    // Leaf node:
    if(!(best instanceof CFNode)) {
      // Threshold constraint satisfied?
      if(abs.squaredDistance(nv, best) <= thresholdsq) {
        best.addToStatistics(nv);
        if(idmap != null) {
          idmap.get(best).add(dbid);
        }
        node.addToStatistics(nv);
        return null;
      }
      best = cfModel.make(nv.getDimensionality());
      if(idmap != null) {
        ArrayModifiableDBIDs list = DBIDUtil.newArray();
        list.add(dbid);
        idmap.put(best, list);
      }
      best.addToStatistics(nv);
      ++leaves;
      if(node.addCF(best)) {
        node.addToStatistics(nv); // Update statistics
        return null;
      }
      return split(node, best);
    }
    assert (best instanceof CFNode) : "Node is neither child nor inner?";
    CFNode<L> newchild = insert((CFNode<L>) best, dbid);
    if(newchild == null || node.addCF(newchild)) {
      node.addToStatistics(nv); // Update statistics
      return null;
    }
    return split(node, (L) newchild);
  }

  /**
   * Find the leaf of a cluster, to get the final cluster assignment.
   * <p>
   * In contrast to {@link #insert}, this does not modify the tree.
   *
   * @param nv Object data
   * @return Leaf this vector should be assigned to
   */
  public L findLeaf(NumberVector nv) {
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
  @SuppressWarnings("unchecked")
  private L findLeaf(CFNode<L> node, NumberVector nv) {
    // Find closest child:
    assert (node.getChild(0) != null) : "Unexpected empty node!";

    // Find the best child:
    L best = node.getChild(0);
    double bestd = dist.squaredDistance(nv, best);
    for(int i = 1; i < capacity; i++) {
      L cf = node.getChild(i);
      if(cf == null) {
        break;
      }
      double d2 = dist.squaredDistance(nv, cf);
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }
    return (best instanceof CFNode) ? findLeaf((CFNode<L>) best, nv) : best;
  }

  /**
   * Split an overfull node.
   *
   * @param node Node to split
   * @param newchild Additional child
   * @return New sibling of {@code node}
   */
  private CFNode<L> split(CFNode<L> node, L newchild) {
    assert (node.getChild(capacity - 1) != null) : "Node to split is not empty!";
    CFNode<L> newn = cfModel.treeNode(node.getDimensionality(), capacity);
    final int size = capacity + 1;
    // Find farthest pair:
    int m1 = -1, m2 = -1;
    double maxd = Double.NEGATIVE_INFINITY;
    double[][] dists = new double[size][size];
    for(int i = 0; i < capacity; i++) {
      L ci = node.getChild(i);
      for(int j = i + 1; j < capacity; j++) {
        double d = dists[i][j] = dists[j][i] = dist.squaredDistance(ci, node.getChild(j));
        if(d > maxd) {
          maxd = d;
          m1 = i;
          m2 = j;
        }
      }
      double d = dists[i][capacity] = dists[capacity][i] = dist.squaredDistance(ci, newchild);
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
        node.addToStatistics(si++, node.getChild(i));
      }
      else {
        newn.addToStatistics(sj++, node.getChild(i));
      }
    }
    // Now also assign the new child:
    double d1 = d1s[capacity], d2 = d2s[capacity];
    if(capacity != m2 && (d1 < d2 || (d1 == d2 && si <= sj))) {
      node.addToStatistics(si++, newchild);
    }
    else {
      newn.addToStatistics(sj++, newchild);
    }
    for(int j = si; j < capacity; j++) {
      node.setChild(j, null);
    }
    for(int j = sj; j < capacity; j++) {
      assert (newn.getChild(j) == null);
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
  @SuppressWarnings("unchecked")
  private CFNode<L> insert(CFNode<L> node, L nleaf) {
    // Find closest child:
    assert (node.getChild(0) != null) : "Unexpected empty node!";

    // Find the best child:
    L best = node.getChild(0);
    double bestd = dist.squaredDistance(best, nleaf);
    for(int i = 1; i < capacity; i++) {
      L cf = node.getChild(i);
      if(cf == null) {
        break;
      }
      double d2 = dist.squaredDistance(cf, nleaf);
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    assert (best != nleaf);
    if(!(best instanceof CFNode)) {
      // Threshold constraint satisfied?
      if(abs.squaredDistance(best, nleaf) <= thresholdsq) {
        best.addToStatistics(nleaf);
        if(idmap != null) {
          idmap.get(best).addDBIDs(idmap.remove(nleaf));
        }
        node.addToStatistics(nleaf);
        return null;
      }
      ++leaves; // We have to add this entry
      if(node.addCF(nleaf)) {
        node.addToStatistics(nleaf); // Update statistics
        return null;
      }
      return split(node, nleaf);
    }
    assert (best instanceof CFNode) : "Node is neither child nor inner?";
    CFNode<L> newchild = insert((CFNode<L>) best, nleaf);
    if(newchild == null || node.addCF(newchild)) {
      node.addToStatistics(nleaf); // Update statistics
      return null;
    }
    return split(node, (L) newchild);
  }

  /**
   * Get an iterator over the leaf nodes.
   *
   * @return Leaf node iterator.
   */
  public LeafIterator<L> leafIterator() {
    return new LeafIterator<>(root);
  }

  /**
   * Iterator over leaf nodes.
   *
   * @author Erich Schubert
   */
  public static class LeafIterator<L extends CFInterface> implements Iter {
    /**
     * Queue of open ends.
     */
    private ArrayList<Object> queue;

    /**
     * Current leaf entry.
     */
    private L current;

    /**
     * Constructor.
     * 
     * @param root Root node
     */
    private LeafIterator(CFNode<L> root) {
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
    public L get() {
      return current;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iter advance() {
      current = null;
      while(!queue.isEmpty()) {
        // Pop last element
        Object f = queue.remove(queue.size() - 1);
        if(!(f instanceof CFNode)) { // lead
          current = (L) f;
          break;
        }
        CFNode<L> node = (CFNode<L>) f;
        for (int i = 0; i < node.capacity(); i++) {
          L c = node.getChild(i);
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
   * Utility function for debugging.
   *
   * @param buf Output buffer
   * @param n Current node
   * @param d Depth
   * @return Output buffer
   */
  protected StringBuilder printDebug(StringBuilder buf, CFInterface n, int d) {
    FormatUtil.appendSpace(buf, d).append(n.getWeight());
    for(int i = 0; i < n.getDimensionality(); i++) {
      buf.append(' ').append(n.centroid(i));
    }
    buf.append(" - ").append(n.getWeight()).append('\n');
    if(n instanceof CFNode) {
      final CFNode<?> node = (CFNode<?>) n;
      for(int i = 0; i < node.capacity(); i++) {
        CFInterface c = node.getChild(i);
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
  public static class Factory<L extends CFInterface> {
    /**
     * Number of tree rebuilds
     */
    long rebuildstat;

    CFModel<L> cfModel;

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
     * Threshold heuristic strategy.
     */
    protected Threshold tCriterium = Threshold.MEAN;

    /**
     * Constructor.
     *
     * @param threshold Distance threshold
     * @param branchingFactor Maximum branching factor.
     * @param maxleaves Maximum number of leaves
     */
    public Factory(CFModel<L> cfModel, double threshold, int branchingFactor, double maxleaves, Threshold tCriterium) {
      this.cfModel = cfModel;
      this.threshold = threshold;
      this.branchingFactor = branchingFactor;
      this.maxleaves = maxleaves;
      this.tCriterium = tCriterium;
    }

    /**
     * Make a new tree.
     *
     * @param ids DBIDs to insert
     * @param relation Data relation
     * @return New tree
     */
    public CFTree<L> newTree(DBIDs ids, Relation<? extends NumberVector> relation, boolean storeIds) {
      rebuildstat = 0;
      Duration buildtime = LOG.newDuration(getClass().getName().replace("$Factory", ".buildtime")).begin();
      CFTree<L> tree = new CFTree<L>(cfModel, relation, threshold, branchingFactor, tCriterium);
      final double max = maxleaves <= 1 ? maxleaves * ids.size() : maxleaves;
      if(storeIds) {
        tree.idmap = new HashMap<CFInterface, ArrayModifiableDBIDs>((int) maxleaves);
      }
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Building tree", relation.size(), LOG) : null;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        tree.insert(it);
        if(tree.leaves > max) {
          if(LOG.isVerbose()) {
            LOG.verbose("Compacting CF-tree.");
          }
          rebuildstat++;
          tree.rebuildTree();
        }
        LOG.incrementProcessed(prog);
      }
      LOG.statistics(buildtime.end());
      LOG.statistics(new LongStatistic(getClass().getName().replace("$Factory", ".rebuilds"), rebuildstat));
      LOG.statistics(new LongStatistic(getClass().getName().replace("$Factory", ".leaves"), tree.leaves));
      LOG.ensureCompleted(prog);
      return tree;
    }

    /**
     * Parameterization class for CFTrees.
     *
     * @author Andreas Lang
     */
    public static class Par<L extends CFInterface> implements Parameterizer {
      /**
       * Option ID for threshold heuristic.
       */
      public static final OptionID SPLIT_ID = new OptionID("cftree.Threshold.heuristic", "Threshold heuristic to use (mean or median).");

      /**
       * Model parameter.
       */
      public static final OptionID MODEL_ID = new OptionID("cftree.model", "CF Model.");

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

      /**
       * CF Model that is used
       */
      CFModel<L> cfModel;

      /**
       * Threshold heuristic strategy.
       */
      protected Threshold tCriterium;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<CFModel<L>>(MODEL_ID, CFModel.class, VVIModel.class) //
            .grab(config, x -> cfModel = x);
        new EnumParameter<Threshold>(SPLIT_ID, Threshold.class, Threshold.MEAN) //
            .grab(config, x -> tCriterium = x);
        new DoubleParameter(THRESHOLD_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
            .setOptional(true) //
            .grab(config, x -> threshold = x);
        new IntParameter(BRANCHING_ID) //
            .addConstraint(new GreaterEqualConstraint(2)) //
            .setDefaultValue(64) //
            .grab(config, x -> branchingFactor = x);
        new DoubleParameter(MAXLEAVES_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .setDefaultValue(0.05) //
            .grab(config, x -> maxleaves = x);
      }

      @Override
      public CFTree.Factory<L> make() {
        return new CFTree.Factory<L>(cfModel, threshold, branchingFactor, maxleaves, tCriterium);
      }
    }
  }

  public int getLeaves() {
    return leaves;
  }

  public CFNode<L> getRoot() {
    return root;
  }

  public int getCapacity() {
    return capacity;
  }
}
