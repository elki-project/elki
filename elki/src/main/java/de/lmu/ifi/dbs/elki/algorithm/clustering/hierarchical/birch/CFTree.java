package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Partial implementation of the CFTree as used by BIRCH.
 * 
 * Important differences:
 * <ol>
 * <li>Leaf entries also store objects</li>
 * <li>Leaf nodes an directory nodes have the same capacity</li>
 * <li>Condensing and memory limits are not implemented</li>
 * <li>Merging refinement (merge-resplit) is not implemented</li>
 * </ol>
 * 
 * Because we want to be able to track the cluster assignments of all data
 * points easily, we need to store the point IDs, and it is not possible to
 * implement the originally proposed page size management at the same time.
 * 
 * Condensing and merging refinement are possible, and improvements are welcome!
 * 
 * TODO: Alternatively, allow bounding the "diameter" instead of the radius?
 * 
 * References:
 * <p>
 * T. Zhang and R. Ramakrishnan and M. Livny<br />
 * BIRCH: An Efficient Data Clustering Method for Very Large Databases
 * Proc. 1996 ACM SIGMOD International Conference on Management of Data
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "T. Zhang and R. Ramakrishnan and M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "http://dx.doi.org/10.1145/233269.233324")
public class CFTree {
  /**
   * Additional reference
   */
  @Reference(authors = "T. Zhang and R. Ramakrishnan and M. Livny", //
      title = "BIRCH: A New Data Clustering Algorithm and Its Applications", //
      booktitle = "Data Min. Knowl. Discovery", //
      url = "http://dx.doi.org/10.1023/A:1009783824328")
  public static final Void ADDITIONAL_REFERENCE = null;

  /**
   * Distance function to use.
   */
  BIRCHDistance distance;

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
   * Constructor.
   *
   * @param distance Distance function to use
   * @param threshold Threshold
   * @param capacity Capacity
   */
  public CFTree(BIRCHDistance distance, double threshold, int capacity) {
    super();
    this.distance = distance;
    this.thresholdsq = threshold * threshold;
    this.capacity = capacity;
  }

  /**
   * Insert a data point into the tree.
   *
   * @param id Object ID
   * @param nv Object data
   */
  public void insert(DBIDRef id, NumberVector nv) {
    final int dim = nv.getDimensionality();
    // No root created yet:
    if(root == null) {
      LeafEntry leaf = new LeafEntry(dim);
      leaf.add(id, nv);
      root = new TreeNode(dim, capacity);
      root.children[0] = leaf;
      root.addToStatistics(nv);
      return;
    }
    TreeNode other = insert(root, id, nv);
    // Handle root overflow:
    if(other != null) {
      TreeNode newnode = new TreeNode(dim, capacity);
      newnode.addToStatistics(newnode.children[0] = root);
      newnode.addToStatistics(newnode.children[1] = other);
      root = newnode;
    }
  }

  /**
   * Recursive insertion.
   *
   * @param node Current node
   * @param id Object id
   * @param nv Object data
   * @return New sibling, if the node was split.
   */
  private TreeNode insert(TreeNode node, DBIDRef id, NumberVector nv) {
    // Find closest child:
    ClusteringFeature[] cfs = node.children;
    assert (cfs[0] != null) : "Unexpected empty node!";

    // Find the best child:
    int best = 0;
    double bestd = distance.distance(nv, cfs[0]);
    for(int i = 1; i < cfs.length; i++) {
      ClusteringFeature cf = cfs[i];
      if(cf == null) {
        break;
      }
      double d2 = distance.distance(nv, cf);
      if(d2 < bestd) {
        best = i;
        bestd = d2;
      }
    }

    ClusteringFeature child = node.children[best];
    if(child instanceof LeafEntry) {
      LeafEntry leaf = (LeafEntry) child;
      // Threshold constraint satisfied?
      LoggingUtil.warning(leaf.n+" "+leaf.diameter()+" "+Math.sqrt(leaf.diameterSqWith(nv)));
      if(leaf.diameterSqWith(nv) <= thresholdsq) {
        leaf.add(id, nv);
        node.addToStatistics(nv);
        assert(leaf.diameter() <= Math.sqrt(thresholdsq)) : leaf.diameter() +" > "+ Math.sqrt(thresholdsq);
        return null;
      }
      leaf = new LeafEntry(nv.getDimensionality());
      leaf.add(id, nv);
      if(add(node.children, leaf)) {
        node.addToStatistics(nv); // Update statistics
        return null;
      }
      return split(node, leaf);
    }
    assert (child instanceof TreeNode) : "Node is neither child nor inner?";
    TreeNode newchild = insert((TreeNode) child, id, nv);
    if(newchild == null || add(node.children, newchild)) {
      node.addToStatistics(nv); // Update statistics
      return null;
    }
    return split(node, newchild);
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
        double d = dists[i][j] = dists[j][i] = distance.distance(ci, node.children[j]);
        if(d > maxd) {
          maxd = d;
          m1 = i;
          m2 = j;
        }
      }
      double d = dists[i][capacity] = dists[capacity][i] = distance.distance(ci, newchild);
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
    return new LeafIterator();
  }

  /**
   * Iterator over leaf nodes.
   *
   * @author Erich Schubert
   */
  public class LeafIterator implements Iter {
    /**
     * Queue of open ends.
     */
    private ArrayList<ClusteringFeature> queue;

    /**
     * Current leaf entry.
     */
    private LeafEntry current;

    /**
     * Constructor.
     */
    private LeafIterator() {
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
    public LeafEntry get() {
      return current;
    }

    @Override
    public Iter advance() {
      current = null;
      while(queue.size() > 0) {
        // Pop last element
        ClusteringFeature f = queue.remove(queue.size() - 1);
        if(f instanceof LeafEntry) {
          current = (LeafEntry) f;
          break;
        }
        assert (f instanceof TreeNode);
        for(ClusteringFeature c : ((TreeNode) f).children) {
          if(c != null) {
            queue.add(c);
          }
        }
      }
      return this;
    }
  }

  /**
   * Leaf entry.
   * 
   * @author Erich Schubert
   */
  public static class LeafEntry extends ClusteringFeature {
    /**
     * Data objects stored in the node.
     */
    // TODO: we store the size twice.
    private ModifiableDBIDs ids;

    /**
     * Constructor.
     *
     * @param dimensionality Dimensionality
     */
    private LeafEntry(int dimensionality) {
      super(dimensionality);
      this.ids = DBIDUtil.newArray();
    }

    /**
     * Add a point to a node.
     * 
     * @param id Object id.
     * @param nv Vector
     */
    private void add(DBIDRef id, NumberVector nv) {
      this.ids.add(id);
      this.addToStatistics(nv);
    }

    /**
     * Get the leaf entry IDs.
     *
     * @return Object IDs
     */
    public DBIDs getIDs() {
      return ids;
    }
  }

  /**
   * Inner node.
   * 
   * @apiviz.exclude
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
    buf.append('\n');
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
     * Cluster merge threshold.
     */
    double threshold;

    /**
     * Maximum branching factor of CFTree.
     */
    int branchingFactor;

    /**
     * Constructor.
     *
     * @param distance Distance to use
     * @param threshold Distance threshold
     * @param branchingFactor Maximum branching factor.
     */
    public Factory(BIRCHDistance distance, double threshold, int branchingFactor) {
      this.distance = distance;
      this.threshold = threshold;
      this.branchingFactor = branchingFactor;
    }

    /**
     * Make a new tree.
     *
     * @return New tree
     */
    public CFTree newTree() {
      return new CFTree(distance, threshold, branchingFactor);
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
      private static final OptionID DISTANCE_ID = new OptionID("cftree.distance", "Distance function to use for node assignment.");

      /**
       * Distance threshold.
       */
      private static final OptionID THRESHOLD_ID = new OptionID("cftree.threshold", "Threshold for adding points to existing nodes in the CF-Tree.");

      /**
       * Branching factor.
       */
      private static final OptionID BRANCHING_ID = new OptionID("cftree.branching", "Maximum branching factor of the CF-Tree");

      /**
       * BIRCH distance function to use
       */
      BIRCHDistance distance;

      /**
       * Cluster merge threshold.
       */
      double threshold;

      /**
       * Maximum branching factor of CFTree.
       */
      int branchingFactor;

      @Override
      protected void makeOptions(Parameterization config) {
        ObjectParameter<BIRCHDistance> distanceP = new ObjectParameter<>(DISTANCE_ID, BIRCHDistance.class, VarianceIncreaseDistance.class);
        if(config.grab(distanceP)) {
          distance = distanceP.instantiateClass(config);
        }

        DoubleParameter thresholdP = new DoubleParameter(THRESHOLD_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
        if(config.grab(thresholdP)) {
          threshold = thresholdP.doubleValue();
        }

        IntParameter branchingP = new IntParameter(BRANCHING_ID) //
            .addConstraint(new GreaterConstraint(2)) //
            .setDefaultValue(64);
        if(config.grab(branchingP)) {
          branchingFactor = branchingP.intValue();
        }
      }

      @Override
      protected CFTree.Factory makeInstance() {
        return new CFTree.Factory(distance, threshold, branchingFactor);
      }
    }
  }
}
