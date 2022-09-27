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
package elki.index.tree.betula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import elki.data.NumberVector;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.index.tree.betula.distance.*;
import elki.index.tree.betula.features.AsClusterFeature;
import elki.index.tree.betula.features.BIRCHCF;
import elki.index.tree.betula.features.ClusterFeature;
import elki.index.tree.betula.features.VIIFeature;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
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

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/**
 * Partial implementation of the CFTree as used by BIRCH and BETULA.
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
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Numerically Stable CF-Trees for BIRCH Clustering<br>
 * Int. Conf on Similarity Search and Applications 2020
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 *
 * @author Erich Schubert
 * @author Andreas Lang
 * @since 0.8.0
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
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Numerically Stable CF-Trees for BIRCH Clustering", //
    booktitle = "Int. Conf on Similarity Search and Applications", //
    url = "https://doi.org/10.1007/978-3-030-60936-8_22", //
    bibkey = "DBLP:conf/sisap/LangS20")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class CFTree<L extends ClusterFeature> {
  /**
   * Class logger.
   */
  public static final Logging LOG = Logging.getLogger(CFTree.class);

  /**
   * Cluster feature factory
   */
  ClusterFeature.Factory<L> factory;

  /**
   * Cluster distance
   */
  CFDistance dist;

  /**
   * Absorption criterion
   */
  CFDistance abs;

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
  Threshold tCriterium;

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
   * Maximum number of leaves allowed
   */
  int maxleaves;

  /**
   * Number of tree rebuilds
   */
  int rebuildstat;

  /**
   * Number of distance calculations
   */
  long diststat = 0;

  /**
   * Number ob absorption calculations
   */
  long absstat = 0;

  /**
   * Stored leaf entry to dbid relation
   */
  Map<ClusterFeature, ArrayModifiableDBIDs> idmap;

  /**
   * Constructor.
   *
   * @param factory Cluster feature factory
   * @param dist Distance function to choose nearest
   * @param abs Absorption criterion
   * @param threshold Distance threshold
   * @param capacity Maximum number of leaves
   * @param tCriterium threshold adjustment rule
   * @param maxleaves Maximum number of leaves
   * @param storeIds Store object ids
   */
  public CFTree(ClusterFeature.Factory<L> factory, CFDistance dist, CFDistance abs, double threshold, int capacity, Threshold tCriterium, int maxleaves, boolean storeIds) {
    super();
    this.factory = factory;
    this.dist = dist;
    this.abs = abs;
    this.thresholdsq = threshold * threshold;
    this.capacity = capacity;
    this.tCriterium = tCriterium;
    this.maxleaves = maxleaves;
    this.idmap = storeIds ? new Reference2ObjectOpenHashMap<ClusterFeature, ArrayModifiableDBIDs>(maxleaves) : null;
  }

  /**
   * Insert a data point into the tree.
   *
   * @param nv Object data
   * @param dbid Object id
   */
  public void insert(NumberVector nv, DBIDRef dbid) {
    // No root created yet:
    if(root == null) {
      final int dim = nv.getDimensionality();
      L leaf = factory.make(dim);
      if(idmap != null) {
        ArrayModifiableDBIDs list = DBIDUtil.newArray();
        list.add(dbid);
        idmap.put(leaf, list);
      }
      leaf.addToStatistics(nv);
      root = new CFNode<>(factory.make(dim), capacity);
      root.add(0, leaf);
      ++leaves;
      return;
    }
    CFNode<L> other = insert(root, nv, dbid);
    // Handle root overflow:
    if(other != null) {
      final int dim = other.getCF().getDimensionality();
      CFNode<L> newnode = new CFNode<>(factory.make(dim), capacity);
      newnode.add(0, root);
      newnode.add(1, other);
      root = newnode;
    }
    if(leaves > maxleaves) {
      if(LOG.isVerbose()) {
        LOG.verbose("Compacting CF-tree.");
      }
      rebuildstat++;
      rebuildTree();
    }
  }

  /**
   * Rebuild the CFTree to condense it to approximately half the size.
   */
  protected void rebuildTree() {
    final int dim = root.getCF().getDimensionality();
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
    root = new CFNode<>(factory.make(dim), capacity);
    root.add(0, cfs.get(order[cfs.size() - 1]));
    ++leaves;
    for(int i = cfs.size() - 2; i >= 0; i--) {
      CFNode<L> other = insert(root, cfs.get(order[i]));
      // Handle root overflow:
      if(other != null) {
        CFNode<L> newnode = new CFNode<>(factory.make(dim), capacity);
        newnode.add(0, root);
        newnode.add(1, other);
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
    if(current.getChild(0) instanceof CFNode) {
      for(int i = 0; i < capacity; i++) {
        if(current.getChild(i) == null) {
          break;
        }
        estimateThreshold((CFNode<L>) current.getChild(i), cfs, thresholds);
      }
      return;
    }
    assert current.getChild(0) instanceof ClusterFeature : "Node is neither child nor inner?";
    if(current.getChild(1) == null) {
      thresholds[offset++] = Double.POSITIVE_INFINITY;
      cfs.add((L) current.getChild(0));
      return;
    }
    double[] best = new double[capacity]; // Cache.
    Arrays.fill(best, Double.POSITIVE_INFINITY);
    int[] besti = new int[capacity];
    for(int i = 0; i < capacity; i++) {
      AsClusterFeature ci = current.getChild(i);
      if(ci == null) {
        break;
      }
      double bi = best[i];
      int bestii = besti[i];
      for(int j = i + 1; j < capacity; j++) {
        if(current.getChild(j) == null) {
          break;
        }
        // use distance for selecting the candidate, not absorption!
        final double d = sqdistance(ci.getCF(), current.getChild(j).getCF());
        if(d < bi) {
          bi = d;
          bestii = j;
        }
        if(d < best[j]) {
          best[j] = d;
          besti[j] = i;
        }
      }
      // use absorption, not distance, for thresholds
      thresholds[offset++] = sqabsorption(ci.getCF(), current.getChild(bestii).getCF());
      cfs.add((L) ci);
    }
  }

  /**
   * Recursive insertion.
   *
   * @param node Current node
   * @param nv Object data
   * @param dbid Object id
   * @return New sibling, if the node was split.
   */
  @SuppressWarnings("unchecked")
  private CFNode<L> insert(CFNode<L> node, NumberVector nv, DBIDRef dbid) {
    assert node.getChild(0) != null : "Unexpected empty node!";
    // Find the best child:
    AsClusterFeature best = node.getChild(0);
    double bestd = sqdistance(nv, best.getCF());
    for(int i = 1; i < capacity; i++) {
      AsClusterFeature cf = node.getChild(i);
      if(cf == null) {
        break;
      }
      final double d2 = sqdistance(nv, cf.getCF());
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    // Leaf node:
    if(!(best instanceof CFNode)) {
      // Threshold constraint satisfied?
      if(sqabsorption(nv, best.getCF()) <= thresholdsq) {
        best.getCF().addToStatistics(nv);
        if(idmap != null) {
          idmap.get((L) best).add(dbid);
        }
        node.getCF().addToStatistics(nv);
        return null;
      }
      L bestl = factory.make(nv.getDimensionality());
      if(idmap != null) {
        ArrayModifiableDBIDs list = DBIDUtil.newArray();
        list.add(dbid);
        idmap.put(bestl, list);
      }
      bestl.addToStatistics(nv);
      ++leaves;
      return node.add(bestl) ? null : split(node, bestl);
    }
    assert (best instanceof CFNode) : "Node is neither child nor inner?";
    CFNode<L> newchild = insert((CFNode<L>) best, nv, dbid);
    if(newchild == null) {
      node.getCF().addToStatistics(nv);
      return null;
    }
    if(node.setChild(newchild)) {
      node.getCF().addToStatistics(nv);
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
    AsClusterFeature best = node.getChild(0);
    double bestd = sqdistance(nv, best.getCF());
    for(int i = 1; i < capacity; i++) {
      AsClusterFeature cf = node.getChild(i);
      if(cf == null) {
        break;
      }
      double d2 = sqdistance(nv, cf.getCF());
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }
    return (best instanceof CFNode) ? findLeaf((CFNode<L>) best, nv) : (L) best;
  }

  /**
   * Split an overfull node.
   *
   * @param node Node to split
   * @param newchild Additional child
   * @return New sibling of {@code node}
   */
  private CFNode<L> split(CFNode<L> node, AsClusterFeature newchild) {
    assert (node.getChild(capacity - 1) != null) : "Node to split is not empty!";
    CFNode<L> newn = new CFNode<>(factory.make(node.getCF().getDimensionality()), capacity);
    final int size = capacity + 1;
    // Find farthest pair:
    int m1 = -1, m2 = -1;
    double maxd = Double.NEGATIVE_INFINITY;
    double[][] dists = new double[size][size];
    for(int i = 0; i < capacity; i++) {
      ClusterFeature ci = node.getChild(i).getCF();
      for(int j = i + 1; j < capacity; j++) {
        double d = dists[i][j] = dists[j][i] = sqdistance(ci, node.getChild(j).getCF());
        if(d > maxd) {
          maxd = d;
          m1 = i;
          m2 = j;
        }
      }
      double d = dists[i][capacity] = dists[capacity][i] = sqdistance(ci, newchild.getCF());
      if(d > maxd) {
        maxd = d;
        m1 = i;
        m2 = capacity;
      }
    }
    // Reset node statistics:
    node.getCF().resetStatistics();
    newn.getCF().resetStatistics();
    // Redistribute entries:
    int si = 0, sj = 0; // Output positions.
    double[] d1s = dists[m1], d2s = dists[m2];
    for(int i = 0; i < capacity; i++) {
      double d1 = d1s[i], d2 = d2s[i];
      if(i == m1 || i != m2 && (d1 < d2 || (d1 == d2 && si <= sj))) {
        node.add(si++, node.getChild(i));
      }
      else {
        newn.add(sj++, node.getChild(i));
      }
    }
    // Now also assign the new child:
    double d1 = d1s[capacity], d2 = d2s[capacity];
    if(capacity != m2 && (d1 < d2 || (d1 == d2 && si <= sj))) {
      node.add(si++, newchild);
    }
    else {
      newn.add(sj++, newchild);
    }
    for(int j = si; j < capacity; j++) {
      node.setChild(j, (L) null);
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
  private CFNode<L> insert(CFNode<L> node, AsClusterFeature nleaf) {
    // Find closest child:
    assert (node.getChild(0) != null) : "Unexpected empty node!";

    // Find the best child:
    AsClusterFeature best = node.getChild(0);
    double bestd = sqdistance(best.getCF(), nleaf.getCF());
    for(int i = 1; i < capacity; i++) {
      AsClusterFeature cf = node.getChild(i);
      if(cf == null) {
        break;
      }
      double d2 = sqdistance(cf.getCF(), nleaf.getCF());
      if(d2 < bestd) {
        best = cf;
        bestd = d2;
      }
    }

    assert (best != nleaf);
    if(!(best instanceof CFNode)) {
      // Threshold constraint satisfied?
      if(sqabsorption(best.getCF(), nleaf.getCF()) <= thresholdsq) {
        best.getCF().addToStatistics(nleaf.getCF());
        if(idmap != null) {
          idmap.get(best).addDBIDs(idmap.remove(nleaf));
        }
        node.getCF().addToStatistics(nleaf.getCF());
        return null;
      }
      ++leaves; // We have to add this entry
      return node.add(nleaf) ? null : split(node, nleaf);
    }
    assert (best instanceof CFNode) : "Node is neither child nor inner?";
    CFNode<L> newchild = insert((CFNode<L>) best, nleaf);
    if(newchild == null) {
      node.getCF().addToStatistics(nleaf.getCF()); // Update statistics upwards
      return null;
    }
    if(node.setChild(newchild)) {
      node.getCF().addToStatistics(nleaf.getCF());
      return null;
    }
    return split(node, newchild);
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
   * Extract the leaves of the tree.
   *
   * @return Leaves
   */
  public ArrayList<L> getLeaves() {
    ArrayList<L> cfs = new ArrayList<>(leaves);
    for(LeafIterator<L> iter = leafIterator(); iter.valid(); iter.advance()) {
      cfs.add(iter.get());
    }
    return cfs;
  }

  /**
   * Iterator over leaf nodes.
   *
   * @author Erich Schubert
   */
  public static class LeafIterator<L extends ClusterFeature> implements Iter {
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
        for(int i = 0; i < node.capacity(); i++) {
          AsClusterFeature c = node.getChild(i);
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
  protected static StringBuilder printDebug(StringBuilder buf, ClusterFeature n, int d) {
    FormatUtil.appendSpace(buf, d).append(n.getWeight());
    for(int i = 0; i < n.getDimensionality(); i++) {
      buf.append(' ').append(n.centroid(i));
    }
    buf.append(" - ").append(n.getWeight()).append('\n');
    if(n instanceof CFNode) {
      final CFNode<?> node = (CFNode<?>) n;
      for(int i = 0; i < node.capacity(); i++) {
        AsClusterFeature c = node.getChild(i);
        if(c != null) {
          printDebug(buf, c.getCF(), d + 1);
        }
      }
    }
    return buf;
  }

  /**
   * Updates statistics and calculates distance between two Cluster Features
   * based on selected criteria.
   * 
   * @param cf1 First Cluster Feature
   * @param cf2 Second Cluster Feature
   * @return Distance
   */
  private double sqdistance(ClusterFeature cf1, ClusterFeature cf2) {
    ++diststat;
    return dist.squaredDistance(cf1, cf2);

  }

  /**
   * Updates statistics and calculates distance between a Number Vector and a
   * Cluster Feature based on selected criteria.
   * 
   * @param nv Number Vector
   * @param cf Cluster Feature
   * @return Distance
   */
  private double sqdistance(NumberVector nv, ClusterFeature cf) {
    ++diststat;
    return dist.squaredDistance(nv, cf);

  }

  /**
   * Updates statistics and calculates distance between two Cluster Features
   * based on selected criteria.
   * 
   * @param cf1 First Cluster Feature
   * @param cf2 Second Cluster Feature
   * @return Distance
   */
  private double sqabsorption(ClusterFeature cf1, ClusterFeature cf2) {
    ++absstat;
    return abs.squaredDistance(cf1.getCF(), cf2.getCF());
  }

  /**
   * Updates statistics and calculates distance between a Number Vector and a
   * Cluster Feature based on selected criteria.
   * 
   * @param nv Number Vector
   * @param cf Cluster Feature
   * @return Distance
   */
  private double sqabsorption(NumberVector nv, ClusterFeature cf) {
    ++absstat;
    return abs.squaredDistance(nv, cf);
  }

  /**
   * Get the number of leaves in the tree.
   * 
   * @return number of leaves
   */
  public int numLeaves() {
    return leaves;
  }

  /**
   * Get the trees root node.
   *
   * @return root node
   */
  public CFNode<L> getRoot() {
    return root;
  }

  /**
   * Get the tree capacity
   * 
   * @return tree capacity
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Get the DBIDs of a cluster feature (if stored).
   * 
   * @param cf Cluster feature
   * @return Object ids
   */
  public DBIDs getDBIDs(ClusterFeature cf) {
    return idmap.get(cf);
  }

  /**
   * CF-Tree Factory.
   *
   * @author Erich Schubert
   */
  public static class Factory<L extends ClusterFeature> {
    /**
     * Cluster feature factory
     */
    ClusterFeature.Factory<L> factory;

    /**
     * BIRCH distance function to use
     */
    CFDistance dist;

    /**
     * BIRCH distance function to use for point absorption
     */
    CFDistance abs;

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
    Threshold tCriterium;

    /**
     * Constructor.
     *
     * @param factory Cluster feature factory
     * @param dist Distance function to choose nearest
     * @param abs Absorption criterion
     * @param threshold Distance threshold
     * @param branchingFactor Maximum branching factor
     * @param maxleaves Maximum number of leaves
     * @param tCriterium threshold adjustment rule
     */
    public Factory(ClusterFeature.Factory<L> factory, CFDistance dist, CFDistance abs, double threshold, int branchingFactor, double maxleaves, Threshold tCriterium) {
      this.factory = factory;
      this.dist = dist;
      this.abs = abs;
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
      final String prefix = CFTree.class.getName();
      Duration buildtime = LOG.newDuration(prefix + ".buildtime").begin();
      final int max = (int) (maxleaves <= 1 ? maxleaves * ids.size() : maxleaves);
      CFTree<L> tree = new CFTree<>(factory, dist, abs, threshold, branchingFactor, tCriterium, max, storeIds);
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Building tree", relation.size(), LOG) : null;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        tree.insert(relation.get(it), it);
        LOG.incrementProcessed(prog);
      }
      LOG.statistics(buildtime.end());
      LOG.statistics(new LongStatistic(prefix + ".rebuilds", tree.rebuildstat));
      LOG.statistics(new LongStatistic(prefix + ".leaves", tree.leaves));
      LOG.statistics(new LongStatistic(prefix + ".distance-calculations", tree.diststat));
      LOG.statistics(new LongStatistic(prefix + ".absorption-calculations", tree.absstat));
      LOG.statistics(new DoubleStatistic(prefix + ".threshold", Math.sqrt(tree.thresholdsq)));
      LOG.ensureCompleted(prog);
      return tree;
    }

    /**
     * Parameterization class for CFTrees.
     *
     * @author Andreas Lang
     * @author Erich Schubert
     */
    public static class Par<L extends ClusterFeature> implements Parameterizer {
      /**
       * Cluster features parameter.
       */
      public static final OptionID FEATURES_ID = new OptionID("cftree.features", "Cluster features to use.");

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
       * Option ID for threshold heuristic.
       */
      public static final OptionID SPLIT_ID = new OptionID("cftree.threshold.heuristic", "Threshold heuristic to use (mean or median).");

      /**
       * Branching factor.
       */
      public static final OptionID BRANCHING_ID = new OptionID("cftree.branching", "Maximum branching factor of the CF-Tree");

      /**
       * Maximum number of leaves.
       */
      public static final OptionID MAXLEAVES_ID = new OptionID("cftree.maxleaves", "Maximum number of leaves (if less than 1, the values is assumed to be relative)");

      /**
       * Cluster feature factory
       */
      ClusterFeature.Factory<L> factory;

      /**
       * BIRCH distance function to use
       */
      CFDistance dist;

      /**
       * BIRCH distance function to use for point absorption
       */
      CFDistance abs;

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
       * Threshold heuristic strategy.
       */
      Threshold tCriterium;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<ClusterFeature.Factory<L>>(FEATURES_ID, ClusterFeature.Factory.class, VIIFeature.Factory.class) //
            .grab(config, x -> factory = x);
        boolean isbirch = factory != null && factory.getClass() == BIRCHCF.Factory.class;
        new ObjectParameter<CFDistance>(DISTANCE_ID, CFDistance.class, isbirch ? BIRCHVarianceIncreaseDistance.class : VarianceIncreaseDistance.class) //
            .grab(config, x -> dist = x);
        new ObjectParameter<CFDistance>(ABSORPTION_ID, CFDistance.class, isbirch ? BIRCHRadiusDistance.class : RadiusDistance.class) //
            .grab(config, x -> abs = x);
        new EnumParameter<Threshold>(SPLIT_ID, Threshold.class, Threshold.MEAN) //
            .grab(config, x -> tCriterium = x);
        new DoubleParameter(THRESHOLD_ID) //
            .setOptional(true) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
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
        return new CFTree.Factory<L>(factory, dist, abs, threshold, branchingFactor, maxleaves, tCriterium);
      }
    }
  }
}
