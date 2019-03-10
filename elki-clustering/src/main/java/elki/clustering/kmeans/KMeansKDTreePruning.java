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
package elki.clustering.kmeans;

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.VectorUtil.SortDBIDsBySingleDimension;
import elki.data.model.KMeansModel;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.distancefunction.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.math.MathUtil;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Pruning K-means with k-d-tree acceleration.
 * <p>
 * This is an implementation of the earlier (1997) version of the k-d-tree based
 * k-means, which uses minimum and maximum distances, rather than pruning with a
 * hyperplane.
 * <p>
 * References:
 * <p>
 * K. Alsabti, S. Ranka, V. Singh<br>
 * An efficient k-means clustering algorithm<br>
 * Electrical Engineering and Computer Science, Technical Report 43
 * <p>
 * K. Alsabti, S. Ranka, V. Singh<br>
 * An Efficient Space-Partitioning Based Algorithm for the K-Means
 * Clustering<br>
 * Pacific-Asia Conference on Knowledge Discovery and Data Mining
 *
 * @author Cedrik Lüdicke (initial version)
 * @author Erich Schubert (optimizations, rewrite)
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "K. Alsabti, S. Ranka, V. Singh", //
    title = "An efficient k-means clustering algorithm", //
    booktitle = "Electrical Engineering and Computer Science, Technical Report 43", //
    url = "https://surface.syr.edu/eecs/43/", //
    bibkey = "tr/syracuse/AlsabtiRS97")
@Reference(authors = "K. Alsabti, S. Ranka, V. Singh", //
    title = "An Efficient Space-Partitioning Based Algorithm for the K-Means Clustering", //
    booktitle = "Pacific-Asia Conference on Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1007/3-540-48912-6_47", //
    bibkey = "DBLP:conf/pakdd/AlsabtiRS99")
public class KMeansKDTreePruning<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansKDTreePruning.class);

  /**
   * Splitting strategies for constructing the k-d-tree.
   *
   * @author Erich Schubert
   */
  public enum Split {
    /**
     * Split halfway between minimum and maximum. This is also sometimes called
     * the "sliding midpoint split", because we use the minimal bounding boxes,
     * not the previous cell.
     */
    MIDPOINT,
    /**
     * K-d-tree typical median split that guarantees minimal height, but tends
     * to produce larger cells.
     */
    MEDIAN
  }

  /**
   * Splitting strategy.
   */
  protected Split split = Split.MIDPOINT;

  /**
   * Desired leaf size.
   */
  protected int leafsize;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param split Splitting strategy
   * @param leafsize Leaf size
   */
  public KMeansKDTreePruning(NumberVectorDistance<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, Split split, int leafsize) {
    super(distanceFunction, k, maxiter, initializer);
    this.split = split;
    this.leafsize = leafsize;
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistance(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Cedrik Lüdicke
   * @author Erich Schubert
   */
  protected class Instance extends AbstractKMeans.Instance {
    /**
     * The root node of the tree
     */
    protected KDNode root;

    /**
     * The tree stored as ArrayModifiableDBIDs
     */
    protected ArrayModifiableDBIDs sorted;

    /**
     * Iterator into the k-d-tree entries.
     */
    protected DBIDArrayMIter iter;

    /**
     * Array of candidate indexes.
     */
    protected int[] indices;

    /**
     * To aggregate the sum of a cluster.
     */
    protected double[][] clusterSums;

    /**
     * Number of elements in each cluster.
     */
    protected int[] clusterSizes;

    /**
     * Constructor.
     *
     * @param relation Relation of data points
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected void run(int maxiter) {
      Duration construction = LOG.newDuration(KMeansKDTreePruning.this.getClass().getName() + ".k-d-tree-construction").begin();
      sorted = DBIDUtil.newArray(relation.getDBIDs());
      iter = sorted.iter();
      switch(split){
      case MIDPOINT:
        root = buildTreeMidpoint(relation, 0, sorted.size());
        break;
      case MEDIAN:
        root = buildTreeMedian(relation, 0, sorted.size(), new VectorUtil.SortDBIDsBySingleDimension(relation));
        break;
      }
      LOG.statistics(construction.end());
      this.indices = MathUtil.sequence(0, k);
      this.clusterSizes = new int[k];
      super.run(maxiter);
    }

    /**
     * Build the k-d-tree.
     *
     * @param relation Data relation
     * @param left Left subinterval
     * @param right Right subinterval
     * @param comp Comparator
     * @return Root node
     */
    public KDNode buildTreeMidpoint(Relation<? extends NumberVector> relation, int left, int right) {
      KDNode node = new KDNode(relation, iter, left, right);
      if(right - left > leafsize) {
        final int dim = VMath.argmax(node.halfwidth);
        double mid = node.mid[dim];
        int l = left, r = right - 1;
        while(true) {
          while(l <= r && relation.get(iter.seek(l)).doubleValue(dim) <= mid) {
            ++l;
          }
          while(l <= r && relation.get(iter.seek(r)).doubleValue(dim) >= mid) {
            --r;
          }
          if(l >= r) {
            break;
          }
          sorted.swap(l++, r--);
        }
        assert relation.get(iter.seek(l)).doubleValue(dim) >= mid : relation.get(iter.seek(l)).doubleValue(dim) + " " + mid;
        if(l < right) { // Duplicate points!
          node.leftChild = buildTreeMidpoint(relation, left, l);
          node.rightChild = buildTreeMidpoint(relation, l, right);
        }
      }
      return node;
    }

    /**
     * Build the k-d-tree.
     *
     * @param relation Data relation
     * @param left Left subinterval
     * @param right Right subinterval
     * @param comp Comparator
     * @return Root node
     */
    public KDNode buildTreeMedian(Relation<? extends NumberVector> relation, int left, int right, SortDBIDsBySingleDimension comp) {
      KDNode node = new KDNode(relation, iter, left, right);
      if(right - left > leafsize) {
        final int middle = (left + right) >>> 1;
        final int sdim = VMath.argmax(node.halfwidth);
        if(node.halfwidth[sdim] > 0) { // Don't split duplicate points.
          comp.setDimension(sdim);
          QuickSelectDBIDs.quickSelect(sorted, comp, left, right, middle);
          node.leftChild = buildTreeMedian(relation, left, middle, comp);
          node.rightChild = buildTreeMedian(relation, middle, right, comp);
        }
      }
      return node;
    }

    @Override
    protected int iterate(int iteration) {
      // recalculate means using the filtering algorithm
      this.clusterSums = new double[means.length][means[0].length];
      Arrays.fill(clusterSizes, 0);
      int changed = traversal(root, indices.length);

      // re-computing means if points were allocated
      for(int k = 0; k < clusterSums.length; k++) {
        if(clusterSizes[k] != 0) {
          means[k] = VMath.timesEquals(clusterSums[k], 1. / clusterSizes[k]);
        }
      }

      // assign clusters if labels did not change or we hit max iteration
      if(changed == 0 || maxiter <= iteration) {
        for(ModifiableDBIDs cluster : clusters) {
          cluster.clear();
        }
        for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
          clusters.get(assignment.intValue(iter)).add(iter);
        }
      }
      return changed;
    }

    /**
     * The tree traversal algorithm.
     *
     * @param u Current node
     * @param alive Number of alive centers
     * @return Number of relabeled point
     */
    protected int traversal(KDNode u, int alive) {
      // Filter the candidate means:
      alive = pruning(u, alive);
      // check if center is found.
      if(alive == 1) {
        return labelSubtree(u.sum, u.start, u.end, indices[0]);
      }
      // Leaf nodes don't have children:
      if(u.leftChild == null) {
        assert (u.rightChild == null);
        return traverseLeaf(u.start, u.end, alive);
      }
      assert u.rightChild != null;
      // recursive call over child nodes.
      return traversal(u.leftChild, alive) + traversal(u.rightChild, alive);
    }

    /**
     * Label an entire subtree.
     *
     * @param sum Node sum
     * @param start Start offset
     * @param end End offset
     * @param index New mean index
     * @return Number of reassigned points (for convergence)
     */
    protected int labelSubtree(double[] sum, int start, int end, int index) {
      // adding coordinate sum and number of associated points to center
      VMath.plusEquals(clusterSums[index], sum);
      clusterSizes[index] += end - start; // Number of points
      // labeling the points of the corresponding subtree
      int changed = 0;
      for(iter.seek(start); iter.getOffset() < end; iter.advance()) {
        // check if point assignments change
        if(assignment.putInt(iter, index) != index) {
          ++changed;
        }
      }
      return changed;
    }

    /**
     * The pruning algorithm.
     * 
     * @param u Current node
     * @param alive Range of alive means
     * @return Updated range.
     */
    protected int pruning(KDNode u, int alive) {
      final double[] mid = u.mid, halfwidth = u.halfwidth;
      double minmaxdist = getMinMaxDist(mid, halfwidth, alive);
      // Filter remaining indexes. Disable by swapping after index range.
      for(int i = 0; i < alive; i++) {
        if(mindist(means[indices[i]], mid, halfwidth) > minmaxdist) {
          --alive;
          final int swap = indices[i];
          indices[i] = indices[alive];
          indices[alive] = swap;
        }
      }
      return alive;
    }

    /**
     * Get the smallest maximum distance for pruning.
     *
     * @param mid Midpoint
     * @param halfwidth Half width
     * @param alive Number of alive centers
     * @return Smallest maximum distance
     */
    protected double getMinMaxDist(double[] mid, double[] halfwidth, int alive) {
      ++diststat;
      int best = 0;
      double bestDistance = Double.POSITIVE_INFINITY;
      for(int i = 0; i < alive; i++) {
        final double[] mean = means[indices[i]];
        double maxdist = 0;
        for(int d = 0; d < mean.length; d++) {
          final double a = mean[d], b = mid[d];
          final double delta = (a > b ? a - b : b - a) + halfwidth[d];
          maxdist += delta * delta;
        }
        if(maxdist < bestDistance) {
          best = i;
          bestDistance = maxdist;
        }
      }
      final int swap = indices[best];
      indices[best] = indices[alive - 1];
      indices[alive - 1] = swap;
      return bestDistance;
    }

    /**
     * Get the smallest maximum distance for pruning.
     *
     * @param mean Cluster mean
     * @param mid Midpoint
     * @param halfwidth Half width
     * @return Minimum distance
     */
    protected double mindist(double[] mean, double[] mid, double[] halfwidth) {
      ++diststat;
      double mindist = 0;
      for(int d = 0; d < mean.length; d++) {
        final double a = mean[d], b = mid[d];
        final double delta = (a > b ? a - b : b - a) - halfwidth[d];
        if(delta > 0) {
          mindist += delta * delta;
        }
      }
      return mindist;
    }

    /**
     * Traversal of a leaf (assuming alive &gt; 1)
     *
     * @param start Start index
     * @param end End index
     * @param alive Number of alive means
     * @return Number of relabeled points (for convergence)
     */
    protected int traverseLeaf(int start, int end, int alive) {
      int changed = 0;
      // find index of nearest Center for all points associated with this leaf
      for(iter.seek(start); iter.getOffset() < end; iter.advance()) {
        int centerIndex = indices[0];
        double currentDistance = Double.POSITIVE_INFINITY;
        NumberVector fv = relation.get(iter);
        for(int i = 0; i < alive; i++) {
          double distance = distance(fv, means[indices[i]]);
          if(distance < currentDistance) {
            centerIndex = indices[i];
            currentDistance = distance;
          }
        }
        clusterSizes[centerIndex]++;
        plusEquals(clusterSums[centerIndex], relation.get(iter));
        if(assignment.putInt(iter, centerIndex) != centerIndex) {
          ++changed;
        }
      }
      return changed;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Node of the k-d-tree used internally.
   * 
   * @author Erich Schubert
   */
  public static class KDNode {
    /**
     * Sum of all points associated with this node
     */
    double[] sum;

    /**
     * Midpoint
     */
    double[] mid;

    /**
     * Half width of the node
     */
    double[] halfwidth;

    /**
     * Left child node
     */
    KDNode leftChild;

    /**
     * Right child node
     */
    KDNode rightChild;

    /**
     * First index of child nodes.
     */
    int start;

    /**
     * End index of child nodes (exclusive).
     */
    int end;

    /**
     * Constructor.
     *
     * @param relation Data
     * @param iter Iterator on the sorted ids.
     * @param start First index
     * @param end Last index (exclusive)
     */
    public KDNode(Relation<? extends NumberVector> relation, DBIDArrayIter iter, int start, int end) {
      this.start = start;
      this.end = end;
      iter.seek(start);
      double[] min = relation.get(iter).toArray(), max = min.clone();
      double[] sum = this.sum = min.clone();
      final int dim = min.length;
      for(iter.advance(); iter.getOffset() < end; iter.advance()) {
        NumberVector currentVector = relation.get(iter);
        for(int i = 0; i < dim; i++) {
          double v = currentVector.doubleValue(i);
          sum[i] += v;
          if(v > max[i]) {
            max[i] = v;
          }
          else if(v < min[i]) {
            min[i] = v;
          }
        }
      }
      // Convert min, max to midpoint +- halfwidth
      for(int i = 0; i < dim; i++) {
        final double mi = min[i], ma = max[i];
        // We're overwriting these temporary arrays:
        min[i] = 0.5 * (ma + mi);
        max[i] = 0.5 * (ma - mi);
      }
      this.mid = min;
      this.halfwidth = max;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    /**
     * Option ID for the splitting strategy.
     */
    public static final OptionID SPLIT_ID = new OptionID("kmeans.kdtree.split", "Splitting strategy to use (midpoint or median).");

    /**
     * Option ID for the leaf size.
     */
    public static final OptionID LEAFSIZE_ID = new OptionID("kmeans.kdtree.leafsize", "Leaf size of the k-d-tree.");

    /**
     * Splitting strategy.
     */
    protected Split split = Split.MIDPOINT;

    /**
     * Desired leaf size.
     */
    protected int leafsize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<Split> splitP = new EnumParameter<>(SPLIT_ID, Split.class, Split.MIDPOINT);
      if(config.grab(splitP)) {
        split = splitP.getValue();
      }
      IntParameter leafsizeP = new IntParameter(LEAFSIZE_ID, 5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(leafsizeP)) {
        leafsize = leafsizeP.intValue();
      }
    }

    @Override
    protected KMeansKDTreePruning<V> makeInstance() {
      return new KMeansKDTreePruning<>(distanceFunction, k, maxiter, initializer, split, leafsize);
    }
  }
}
