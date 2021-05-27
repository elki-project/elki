/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.index.tree.metrical.vptree;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.DistancePriorityIndex;
import elki.index.IndexFactory;
import elki.index.tree.metrical.covertree.CoverTree;
import elki.logging.Logging;
import elki.logging.statistics.Counter;
import elki.utilities.Alias;
import elki.utilities.datastructures.heap.ComparableMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Vantage Point Tree with no additional information
 * <p>
 * In a Vantage Point Tree the Data is split at the median distance to a vantage
 * point at every node.
 * <p>
 * The distance function in the original paper was bounded to have a limited tau
 * size. In this class the function is not bounded, as we can just limit tau to
 * Double.MAX_VALUE
 * <p>
 * Reference:
 * <p>
 * Peter N. Yianilos<br>
 * Data Structures and Algorithms for Nearest Neighbor Search in General Metric Spaces<br>
 * Proc. of the Fourth Annual {ACM/SIGACT-SIAM} Symposium on Discrete Algorithms (1993)
 * 
 * 
 * @author Robert Gehde
 *
 * @param <O>
 *
 */
@Reference(authors = "Peter N. Yianilos", //
    booktitle = "Proceedings of the Fourth Annual {ACM/SIGACT-SIAM} Symposium on Discrete Algorithms, 25-27 January 1993, Austin, Texas, {USA}", //
    title = "Data Structures and Algorithms for Nearest Neighbor Search in General Metric Spaces", //
    url = "http://dl.acm.org/citation.cfm?id=313559.313789", //
    bibkey = "DBLP:conf/soda/Yianilos93")
public class VPTree<O> implements DistancePriorityIndex<O> {

  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CoverTree.class);

  /**
   * Counter for comparisons.
   */
  protected final Counter objaccess;

  /**
   * Counter for distance computations.
   */
  protected final Counter distcalc;

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * Distance Function to use
   */
  Distance<O> distFunc;

  /**
   * Actual distance query on the Data
   */
  DistanceQuery<O> distQuery;

  /**
   * Distance storage for building
   */
  ModifiableDoubleDBIDList sorted;

  /**
   * Random factory for selecting vantage points
   */
  RandomFactory random;

  /**
   * Sample size for selecting vantage points
   */
  int sampleSize;

  /**
   * Root node from the tree
   */
  Node root;

  /**
   * The Node Class saves the important information for the each Node
   * 
   * @author Robert Gehde
   *
   */
  static class Node {
    /**
     * vantage point
     */
    DBID vp;

    /**
     * child trees
     */
    Node leftChild, rightChild;

    /**
     * left and right bound in the sorted DBIDList
     */
    int leftBound, rightBound;

    /**
     * upper and lower distance bound for the left and right subtree
     */
    double leftLowBound, leftHighBound, rightLowBound, rightHighBound;

    public Node(int left, int right) {
      this.leftBound = left;
      this.rightBound = right;
      this.leftLowBound = Double.MAX_VALUE;
      this.rightLowBound = Double.MAX_VALUE;
      this.leftHighBound = Double.MIN_VALUE;
      this.rightHighBound = Double.MIN_VALUE;
    }
  }

  /**
   * 
   * Constructor.
   *
   * @param relation data for tree construction
   * @param distance distance function for tree construction
   */
  public VPTree(Relation<O> relation, Distance<O> distance, RandomFactory random, int sampleSize) {
    this.relation = relation;
    this.distFunc = distance;
    this.random = random;
    this.distQuery = distance.instantiate(relation);
    this.sampleSize = sampleSize;
    if(LOG.isStatistics()) {
      String prefix = this.getClass().getName();
      this.objaccess = LOG.newCounter(prefix + ".objaccess");
      this.distcalc = LOG.newCounter(prefix + ".distancecalcs");
    }
    else {
      this.objaccess = null;
      this.distcalc = null;
    }
  }

  @Override
  public void initialize() {
    sorted = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      sorted.add(Double.NaN, it);
    }
    root = new Node(0, relation.size());
    buildTree(root, 0, relation.size(), sorted.iter());
  }

  /**
   * builds the tree recursively
   * 
   * @param current current node to build
   * @param left left bound of the node
   * @param right right bound of the node
   * @param iter reference to the sorted DBIDs
   */
  private void buildTree(Node current, int left, int right, DoubleDBIDListMIter iter) {
    // find vantage point
    DBID vantagePoint = findVantagePoint(left, right, iter);
    current.vp = vantagePoint;
    // find median in dist(vp, rel)
    int vantagePointOffset = 0;
    for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
      double d = distQuery.distance(iter, vantagePoint);
      countDistanceComputation();
      iter.setDouble(d);
      countObjectAccess();
      if(DBIDUtil.compare(iter, vantagePoint) == 0) {
        vantagePointOffset = iter.getOffset();
      }
    }
    if(left + 1 == right) {
      // node only has vp, the rest
      current.leftLowBound = Double.POSITIVE_INFINITY;
      current.rightLowBound = Double.POSITIVE_INFINITY;
      current.leftHighBound = Double.NEGATIVE_INFINITY;
      current.rightHighBound = Double.NEGATIVE_INFINITY;
    }
    else {
      int middle = (left + 1 + right) >>> 1;
      // sort left < median; right >= median
      // quickselect only assures that the median is correct
      // exclude current vantage point
      sorted.swap(left, vantagePointOffset);
      countObjectAccess();
      QuickSelectDBIDs.quickSelect(sorted, left + 1, right, middle);

      // offset for values == median, such that correct sorting is given
      double median = iter.seek(middle).doubleValue();
      countObjectAccess();
      for(iter.seek(left + 1); iter.getOffset() < middle; iter.advance()) {
        double d = iter.doubleValue();
        if(d == median) {
          sorted.swap(iter.getOffset(), --middle);
          countObjectAccess();
        }
        else {
          if(d < current.leftLowBound) {
            current.leftLowBound = d;
          }
          if(d > current.leftHighBound) {
            current.leftHighBound = d;
          }
        }
      }
      for(iter.seek(middle); iter.getOffset() < right; iter.advance()) {
        double d = iter.doubleValue();
        if(d < current.rightLowBound) {
          current.rightLowBound = d;
        }
        if(d > current.rightHighBound) {
          current.rightHighBound = d;
        }
      }
      // construct child trees
      if(middle - (left + 1) > 0) {
        current.leftChild = new Node(left + 1, middle);
        buildTree(current.leftChild, left + 1, middle, iter);
      }
      if(right - middle > 0) {
        current.rightChild = new Node(middle, right);
        buildTree(current.rightChild, middle, right, iter);
      }
    }
  }

  /**
   * 
   * finds a vantage points in the DBIDs between left and right
   * 
   * @param left left bound
   * @param right right bound
   * @param iter DBIDs to find a vantage point in
   * @return vantage point
   */
  private DBID findVantagePoint(int left, int right, DoubleDBIDListMIter iter) {
    // init, construct workset
    int s = Math.min(sampleSize, right - left);
    ArrayModifiableDBIDs workset = DBIDUtil.newArray(right - left);
    for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
      workset.add(iter);
      countObjectAccess();
    }
    // find vantage point
    ModifiableDBIDs candidates = DBIDUtil.randomSample(workset, s, random);
    DBID best = null;
    double bestSpread = Double.NEGATIVE_INFINITY;
    for(DBIDMIter it = candidates.iter(); it.valid(); it.advance()) {
      countObjectAccess();
      DBIDs check = DBIDUtil.randomSample(workset, s, random);
      // calculate moment
      double spread = calcMoment(check, it, s);
      if(spread > bestSpread) {
        bestSpread = spread;
        best = DBIDUtil.deref(it);
      }
    }
    return best;
  }

  /**
   * calculates the 2nd moment to the median of the point it to the points in
   * check
   * 
   * @param check points to check with
   * @param it DBID to calculate the moment for
   * @param s sample size of check
   * @return calculated moment
   */
  private double calcMoment(DBIDs check, DBIDIter it, int s) {
    // calc distances
    ModifiableDoubleDBIDList vals = DBIDUtil.newDistanceDBIDList(s);
    for(DBIDIter iter = check.iter(); iter.valid(); iter.advance()) {
      double d = distQuery.distance(it, iter);
      countDistanceComputation();
      // d = d / (d + 1);
      vals.add(d, iter);
    }
    // calc median
    int pos = QuickSelectDBIDs.median(vals);
    double median = vals.iter().seek(pos).doubleValue();
    // calc 2nd moment
    double moment = 0;
    for(DoubleDBIDListMIter iter = vals.iter(); iter.valid(); iter.advance()) {
      moment += Math.pow(iter.doubleValue() - median, 2);
    }
    return moment / s;
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    Distance<? super O> df = distanceQuery.getDistance();
    // only should work for same distance as construction distance
    if(df.equals(distFunc)) {
      return new VPTreeKNNSearcher();
    }
    return null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    Distance<? super O> df = distanceQuery.getDistance();
    // only should work for same distance as construction distance
    if(df.equals(distFunc)) {
      return new VPTreeRangeSearcher();
    }
    return null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    Distance<? super O> df = distanceQuery.getDistance();
    // only should work for same distance as construction distance
    if(df.equals(distFunc)) {
      return new VPTreePrioritySearcher();
    }
    return null;
  }

  /**
   * kNN query for the vp-tree.
   *
   * @author Robert Gehde
   */
  public class VPTreeKNNSearcher implements KNNSearcher<O> {

    @Override
    public KNNList getKNN(O obj, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      vpKNNSearch(obj, knns, root, Double.MAX_VALUE);
      return knns.toKNNList();
    }

    private double vpKNNSearch(O obj, KNNHeap knns, Node node, double tau) {
      if(node == null) {
        return tau;
      }
      double x = distQuery.distance(node.vp, obj);
      countDistanceComputation();
      if(x < tau) {
        knns.insert(x, node.vp);
        tau = knns.getKNNDistance();
      }
      double middle = (node.leftHighBound + node.rightLowBound) / 2;

      if(x < middle) {
        if(x > node.leftLowBound - tau && x < node.leftHighBound + tau) {
          tau = vpKNNSearch(obj, knns, node.leftChild, tau);
        }
        if(x > node.rightLowBound - tau && x < node.rightHighBound + tau) {
          tau = vpKNNSearch(obj, knns, node.rightChild, tau);
        }
      }
      else {
        if(x > node.rightLowBound - tau && x < node.rightHighBound + tau) {
          tau = vpKNNSearch(obj, knns, node.rightChild, tau);
        }
        if(x > node.leftLowBound - tau && x < node.leftHighBound + tau) {
          tau = vpKNNSearch(obj, knns, node.leftChild, tau);
        }
      }
      return tau;
    }
  }

  public class VPTreeRangeSearcher implements RangeSearcher<O> {

    @Override
    public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
      vpRangeSearch(query, result, root, range);
      return result;
    }

    private void vpRangeSearch(O query, ModifiableDoubleDBIDList result, Node node, double range) {
      if(node == null) {
        return;
      }
      double x = distQuery.distance(node.vp, query);
      countDistanceComputation();
      if(x <= range) {
        result.add(x, node.vp);
      }
      double middle = (node.leftHighBound + node.rightLowBound) / 2;

      if(x < middle) {
        if(x > node.leftLowBound - range && x < node.leftHighBound + range) {
          vpRangeSearch(query, result, node.leftChild, range);
        }
        if(x > node.rightLowBound - range && x < node.rightHighBound + range) {
          vpRangeSearch(query, result, node.rightChild, range);
        }
      }
      else {
        if(x > node.rightLowBound - range && x < node.rightHighBound + range) {
          vpRangeSearch(query, result, node.rightChild, range);
        }
        if(x > node.leftLowBound - range && x < node.leftHighBound + range) {
          vpRangeSearch(query, result, node.leftChild, range);
        }
      }
    }
  }

  /**
   * Search position for priority search.
   *
   * @author Robert Gehde
   */
  private class PrioritySearchBranch implements Comparable<PrioritySearchBranch> {
    /**
     * Minimum distance
     */
    double mindist;

    /**
     * Node
     */
    Node node;

    /**
     * Constructor.
     *
     * @param mindist Minimum distance
     * @param left Interval begin
     * @param right Interval end (exclusive)
     * @param axis Next axis
     */
    public PrioritySearchBranch(double mindist, Node node) {
      this.mindist = mindist;
      this.node = node;
    }

    @Override
    public int compareTo(PrioritySearchBranch o) {
      return Double.compare(this.mindist, o.mindist);
    }
  }

  /**
   * 
   * @author Robert Gehde
   *
   */
  public class VPTreePrioritySearcher implements PrioritySearcher<O> {

    /**
     * Min heap for searching.
     */
    private ComparableMinHeap<PrioritySearchBranch> heap = new ComparableMinHeap<>();

    /**
     * Current query object.
     */
    private O query;

    /**
     * Stopping threshold.
     */
    private double threshold;

    /**
     * Current search position.
     */
    private PrioritySearchBranch cur;

    @Override
    public PrioritySearcher<O> search(O query) {
      this.query = query;
      this.threshold = Double.POSITIVE_INFINITY;
      this.heap.clear();
      this.heap.add(new PrioritySearchBranch(0, root));
      return advance();
    }

    @Override
    public PrioritySearcher<O> advance() {
      if(heap.isEmpty()) {
        cur = null;
        return this;
      }
      // Get next
      cur = heap.poll();
      if(cur.mindist > threshold) {
        cur = null;
        return this;
      }
      // border value
      double middist = (cur.node.leftHighBound + cur.node.rightLowBound) / 2;

      // Distance to axis:
      final double delta = middist - distQuery.distance(query, cur.node.vp);
      countDistanceComputation();
      final double mindist = distFunc.isSquared() ? delta * delta : Math.abs(delta);

      // Next axis:
      final double ldist = delta < 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
      if(cur.node.leftChild != null && ldist <= threshold) {
        heap.add(new PrioritySearchBranch(ldist, cur.node.leftChild));
      }
      final double rdist = delta > 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
      if(cur.node.rightChild != null && rdist <= threshold) {
        heap.add(new PrioritySearchBranch(rdist, cur.node.rightChild));
      }
      return this;
    }

    @Override
    public int internalGetIndex() {
      return cur.node.vp.internalGetIndex();
    }

    @Override
    public boolean valid() {
      return cur != null;
    }

    @Override
    public PrioritySearcher<O> decreaseCutoff(double threshold) {
      assert threshold <= this.threshold : "Thresholds must only decreasee.";
      this.threshold = threshold;
      return this;
    }

    @Override
    public double computeExactDistance() {
      return distQuery.distance(query, relation.get(cur.node.vp));
    }

    @Override
    public double allLowerBound() {
      return cur.mindist;
    }

    @Override
    public double getLowerBound() {
      return cur.mindist;
    }
  }

  /**
   * Count a single object access.
   */
  protected void countObjectAccess() {
    if(objaccess != null) {
      objaccess.increment();
    }
  }

  /**
   * Count a distance computation.
   */
  protected void countDistanceComputation() {
    if(distcalc != null) {
      distcalc.increment();
    }
  }

  @Override
  public void logStatistics() {
    if(objaccess != null) {
      LOG.statistics(objaccess);
    }
    if(distcalc != null) {
      LOG.statistics(distcalc);
    }
  }

  @Alias({ "vp" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O> {

    /**
     * Distance Function
     */
    Distance<O> distance;

    /**
     * Random factory
     */
    RandomFactory random;

    /**
     * Sample size
     */
    int sampleSize;

    /**
     * 
     * Constructor.
     *
     */
    @SuppressWarnings("unchecked")
    public Factory() {
      this((Distance<O>) EuclideanDistance.STATIC, RandomFactory.DEFAULT, 10);
    }

    /**
     * Constructor.
     *
     */
    public Factory(Distance<O> distFunc, RandomFactory random, int sampleSize) {
      super();
      this.distance = distFunc;
      this.random = random;
      this.sampleSize = sampleSize;
    }

    @Override
    public VPTree<O> instantiate(Relation<O> relation) {
      return new VPTree<>(relation, distance, random, sampleSize);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     *
     * @author Robert Gehde
     */
    public static class Par<O extends NumberVector> implements Parameterizer {
      /**
       * Parameter to specify the distance function to determine the distance
       * between database objects, must extend
       * {@link elki.distance.Distance}.
       */
      public final static OptionID DISTANCE_FUNCTION_ID = new OptionID("vptree.distanceFunction", "Distance function to determine the distance between objects.");

      /**
       * Parameter to specify the sample size for choosing vantage point
       */
      public final static OptionID SAMPLE_SIZE_ID = new OptionID("vptree.sampleSize", "Size of sample to select vantage point from.");

      /**
       * Parameter to specify the random generator seed
       */
      public final static OptionID SEED_ID = new OptionID("vptree.seed", "The random number generator seed.");

      /**
       * Distance function
       */
      protected Distance<O> distance;

      /**
       * Random generator
       */
      protected RandomFactory random;

      /**
       * Sample size
       */
      protected int sampleSize;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<O>>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class)//
            .grab(config, x -> {
              this.distance = x;
            });
        new IntParameter(SAMPLE_SIZE_ID, 10) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)//
            .grab(config, x -> {
              this.sampleSize = x;
            });
        new RandomParameter(SEED_ID).grab(config, x -> random = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(distance, random, sampleSize);
      }
    }
  }
}
