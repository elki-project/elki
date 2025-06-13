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
package elki.index.tree.metrical.vptree;

import java.util.Random;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.SquaredPrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.SquaredKNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.query.range.SquaredRangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.DistancePriorityIndex;
import elki.index.IndexFactory;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.utilities.Alias;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.datastructures.heap.DoubleObjectMinHeap;
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
 * In a Vantage Point Tree the data is split at the median distance to a vantage
 * point at every node.
 * <p>
 * The distance function in the original paper was bounded to have a limited tau
 * size. In this class the function is not bounded, as we can just limit tau to
 * Double.MAX_VALUE
 * <p>
 * Reference:
 * <p>
 * P. N. Yianilos<br>
 * Data Structures and Algorithms for Nearest Neighbor Search in General Metric
 * Spaces<br>
 * Proc. ACM/SIGACT-SIAM Symposium on Discrete Algorithms
 * 
 * @author Robert Gehde
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type
 */
@Reference(authors = "P. N. Yianilos", //
    title = "Data Structures and Algorithms for Nearest Neighbor Search in General Metric Spaces", //
    booktitle = "Proc. ACM/SIGACT-SIAM Symposium on Discrete Algorithms", //
    url = "http://dl.acm.org/citation.cfm?id=313559.313789", //
    bibkey = "DBLP:conf/soda/Yianilos93")
public class VPTree<O> implements DistancePriorityIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(VPTree.class);

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * Distance Function to use
   */
  Distance<? super O> distFunc;

  /**
   * Actual distance query on the Data
   */
  private DistanceQuery<O> distQuery;

  /**
   * Random factory for selecting vantage points
   */
  RandomFactory random;

  /**
   * Sample size for selecting vantage points
   */
  int sampleSize;

  /**
   * Truncation parameter
   */
  int truncate;

  /**
   * Counter for distance computations.
   */
  long distComputations = 0L;

  /**
   * Root node from the tree
   */
  Node root;

  /**
   * Constructor with default values, used by EmpiricalQueryOptimizer
   *
   * @param relation data for tree construction
   * @param distance distance function for tree construction
   * @param leafsize Leaf size and sample size (simpler parameterization)
   */
  public VPTree(Relation<O> relation, Distance<? super O> distance, int leafsize) {
    this(relation, distance, RandomFactory.DEFAULT, leafsize, leafsize);
  }

  /**
   * Constructor.
   *
   * @param relation data for tree construction
   * @param distance distance function for tree construction
   * @param random Random generator for sampling
   * @param sampleSize Sample size for finding the vantage point
   * @param truncate Leaf size threshold
   */
  public VPTree(Relation<O> relation, Distance<? super O> distance, RandomFactory random, int sampleSize, int truncate) {
    this.relation = relation;
    this.distFunc = distance;
    this.random = random;
    this.distQuery = distance.instantiate(relation);
    this.sampleSize = Math.max(sampleSize, 1);
    this.truncate = Math.max(truncate, 1);
  }

  @Override
  public void initialize() {
    root = new Builder().buildTree(0, relation.size());
  }

  /**
   * Build the VP-Tree
   *
   * @author Erich Schubert
   */
  private class Builder {
    /**
     * Scratch space for organizing the elements
     */
    ModifiableDoubleDBIDList scratch;

    /**
     * Scratch iterator
     */
    DoubleDBIDListMIter scratchit;

    /**
     * Random generator
     */
    Random rnd;

    /**
     * Constructor.
     */
    public Builder() {
      scratch = DBIDUtil.newDistanceDBIDList(relation.size());
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        scratch.add(Double.NaN, it);
      }
      scratchit = scratch.iter();
      rnd = VPTree.this.random.getSingleThreadedRandom();
    }

    /**
     * Build the tree recursively
     * 
     * @param left Left bound in scratch
     * @param right Right bound in scratch
     * @return new node
     */
    private Node buildTree(int left, int right) {
      assert left < right;
      if(left + truncate >= right) {
        DBID vp = DBIDUtil.deref(scratchit.seek(left));
        ModifiableDoubleDBIDList vps = DBIDUtil.newDistanceDBIDList(right - left);
        vps.add(0., vp);
        for(scratchit.advance(); scratchit.getOffset() < right; scratchit.advance()) {
          vps.add(distance(vp, scratchit), scratchit);
        }
        return new Node(vps);
      }
      DBIDVar vantagePoint = chooseVantagePoint(left, right);
      int tied = 0;
      // Compute all the distances to the best vantage point (not just sample)
      for(scratchit.seek(left); scratchit.getOffset() < right; scratchit.advance()) {
        if(DBIDUtil.equal(scratchit, vantagePoint)) {
          scratchit.setDouble(0);
          if(tied > 0 && scratchit.getOffset() != left + tied) {
            scratch.swap(left, left + tied);
          }
          scratch.swap(scratchit.getOffset(), left);
          tied++;
          continue;
        }
        final double d = distance(scratchit, vantagePoint);
        scratchit.setDouble(d);
        if(d == 0) {
          scratch.swap(scratchit.getOffset(), left + tied++);
        }
      }
      assert tied > 0;
      assert DBIDUtil.equal(vantagePoint, scratchit.seek(left)) : "tied: " + tied;
      // Note: many duplicates of vantage point:
      if(left + tied + truncate > right) {
        ModifiableDoubleDBIDList vps = DBIDUtil.newDistanceDBIDList(right - left);
        for(scratchit.seek(left); scratchit.getOffset() < right; scratchit.advance()) {
          vps.add(scratchit.doubleValue(), scratchit);
        }
        return new Node(vps);
      }
      int middle = (left + tied + right) >>> 1;
      // sort left < median; right >= median
      // quickselect only assures that the median is correct
      // exclude current vantage point
      QuickSelectDBIDs.quickSelect(scratch, left + tied, right, middle);
      final double median = scratch.doubleValue(middle);

      // offset for values == median, such that correct sorting is given
      double leftLowBound = Double.POSITIVE_INFINITY;
      double rightLowBound = Double.POSITIVE_INFINITY;
      double leftHighBound = Double.NEGATIVE_INFINITY;
      double rightHighBound = Double.NEGATIVE_INFINITY;
      for(scratchit.seek(left + tied); scratchit.getOffset() < middle; scratchit.advance()) {
        final double d = scratchit.doubleValue();
        // Move all tied with the median to the second partition
        if(d == median) {
          scratch.swap(scratchit.getOffset(), --middle);
          continue;
        }
        leftLowBound = d < leftLowBound ? d : leftLowBound;
        leftHighBound = d > leftHighBound ? d : leftHighBound;
      }
      for(scratchit.seek(middle); scratchit.getOffset() < right; scratchit.advance()) {
        final double d = scratchit.doubleValue();
        rightLowBound = d < rightLowBound ? d : rightLowBound;
        rightHighBound = d > rightHighBound ? d : rightHighBound;
      }
      assert right > middle;
      // Recursive build, include ties with parent:
      ModifiableDoubleDBIDList vps = DBIDUtil.newDistanceDBIDList(tied);
      for(scratchit.seek(left); scratchit.getOffset() < left + tied; scratchit.advance()) {
        vps.add(scratchit.doubleValue(), scratchit);
      }
      Node current = new Node(vps);
      // Note: left branch may disappear if the medoid is tied often
      if(left + tied < middle) {
        current.leftChild = buildTree(left + tied, middle);
        current.leftChild.lowBound = leftLowBound;
        current.leftChild.highBound = leftHighBound;
      }
      current.rightChild = buildTree(middle, right);
      current.rightChild.lowBound = rightLowBound;
      current.rightChild.highBound = rightHighBound;
      return current;
    }

    /**
     * Find a vantage points in the DBIDs between left and right
     * 
     * @param left Left bound in scratch
     * @param right Right bound in scratch
     * @return vantage point
     */
    private DBIDVar chooseVantagePoint(int left, int right) {
      // Random sampling:
      if(sampleSize == 1) {
        return scratch.assignVar(left + rnd.nextInt(right - left), DBIDUtil.newVar());
      }
      final int s = Math.min(sampleSize, right - left);
      double bestSpread = Double.NEGATIVE_INFINITY;
      DBIDVar best = DBIDUtil.newVar();
      // Modifiable copy for sampling:
      ArrayModifiableDBIDs workset = DBIDUtil.newArray(right - left);
      for(scratchit.seek(left); scratchit.getOffset() < right; scratchit.advance()) {
        workset.add(scratchit);
      }
      for(DBIDMIter it = DBIDUtil.randomSample(workset, s, rnd).iter(); it.valid(); it.advance()) {
        // Sample s+1 objects in case `it` is contained.
        DBIDUtil.randomShuffle(workset, rnd, Math.min(s + 1, workset.size()));
        double spread = calcMoment(it, workset, s);
        if(spread > bestSpread) {
          bestSpread = spread;
          best.set(it);
        }
      }
      return best;
    }

    /**
     * Calculate the 2nd moment to the median of the distances to p
     * 
     * @param p DBID to calculate the moment for
     * @param check points to check with
     * @param size Maximum size to use
     * @return second moment
     */
    private double calcMoment(DBIDRef p, DBIDs check, int size) {
      double[] dists = new double[Math.min(size, check.size())];
      int i = 0;
      for(DBIDIter iter = check.iter(); iter.valid() && i < size; iter.advance()) {
        if(!DBIDUtil.equal(iter, p)) {
          dists[i++] = distance(p, iter);
        }
      }
      double median = QuickSelect.median(dists);
      double ssq = 0;
      for(int j = 0; j < i; j++) {
        final double o = dists[j] - median;
        ssq += o * o;
      }
      return ssq / i;
    }
  }

  /**
   * The Node Class saves the important information for the each Node
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  protected static class Node {
    /**
     * Vantage point and singletons
     */
    ModifiableDoubleDBIDList vp;

    /**
     * child trees
     */
    Node leftChild, rightChild;

    /**
     * upper and lower distance bounds
     */
    double lowBound, highBound;

    /**
     * Constructor.
     * 
     * @param vp Vantage point and singletons
     */
    public Node(ModifiableDoubleDBIDList vp) {
      this.vp = vp;
      this.lowBound = Double.NaN;
      this.highBound = Double.NaN;
      assert !vp.isEmpty();
    }
  }

  /**
   * Compute a distance, and count.
   *
   * @param a First object
   * @param b Second object
   * @return Distance
   */
  private double distance(DBIDRef a, DBIDRef b) {
    ++distComputations;
    return distQuery.distance(a, b);
  }

  /**
   * Compute a distance, and count.
   *
   * @param a First object
   * @param b Second object
   * @return Distance
   */
  private double distance(O a, DBIDRef b) {
    ++distComputations;
    return distQuery.distance(a, b);
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(this.distFunc.getClass() == EuclideanDistance.class && distanceQuery.getDistance().getClass() == SquaredEuclideanDistance.class) {
      return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
          distanceQuery.getRelation() == relation ? new SquaredKNNSearcher<>(new VPTreeKNNObjectSearcher()) : null;
    }
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new VPTreeKNNObjectSearcher() : null;
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(this.distFunc.getClass() == EuclideanDistance.class && distanceQuery.getDistance().getClass() == SquaredEuclideanDistance.class) {
      return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
          distanceQuery.getRelation() == relation ? new SquaredKNNSearcher<>(new VPTreeKNNDBIDSearcher()) : null;
    }
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new VPTreeKNNDBIDSearcher() : null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    if(this.distFunc.getClass() == EuclideanDistance.class && distanceQuery.getDistance().getClass() == SquaredEuclideanDistance.class) {
      return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
          distanceQuery.getRelation() == relation ? new SquaredRangeSearcher<>(new VPTreeRangeObjectSearcher()) : null;
    }
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new VPTreeRangeObjectSearcher() : null;
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    if(this.distFunc.getClass() == EuclideanDistance.class && distanceQuery.getDistance().getClass() == SquaredEuclideanDistance.class) {
      return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
          distanceQuery.getRelation() == relation ? new SquaredRangeSearcher<>(new VPTreeRangeDBIDSearcher()) : null;
    }
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new VPTreeRangeDBIDSearcher() : null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    if(this.distFunc.getClass() == EuclideanDistance.class && distanceQuery.getDistance().getClass() == SquaredEuclideanDistance.class) {
      return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
          distanceQuery.getRelation() == relation ? new SquaredPrioritySearcher<>(new VPTreePriorityObjectSearcher()) : null;
    }
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new VPTreePriorityObjectSearcher() : null;
  }

  @Override
  public PrioritySearcher<DBIDRef> priorityByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    if(this.distFunc.getClass() == EuclideanDistance.class && distanceQuery.getDistance().getClass() == SquaredEuclideanDistance.class) {
      return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
          distanceQuery.getRelation() == relation ? new SquaredPrioritySearcher<>(new VPTreePriorityDBIDSearcher()) : null;
    }
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distFunc.equals(distanceQuery.getDistance()) ? //
            new VPTreePriorityDBIDSearcher() : null;
  }

  /**
   * kNN search for the VP-Tree.
   *
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public static abstract class VPTreeKNNSearcher {
    /**
     * Recursive search function
     * 
     * @param knns Current kNN results
     * @param node Current node
     * @return New tau
     */
    protected double vpKNNSearch(KNNHeap knns, Node node) {
      DoubleDBIDListIter vp = node.vp.iter();
      final double x = queryDistance(vp);
      knns.insert(x, vp);
      for(vp.advance(); vp.valid(); vp.advance()) {
        knns.insert(queryDistance(vp), vp);
      }
      // Prioritize left or right branch:
      Node lc = node.leftChild, rc = node.rightChild;
      double tau = knns.getKNNDistance();
      if(lc == null || rc == null || x < (lc.highBound + rc.lowBound) * 0.5) {
        if(lc != null && lc.lowBound <= x + tau && x - tau <= lc.highBound) {
          tau = vpKNNSearch(knns, lc);
        }
        if(rc != null && rc.lowBound <= x + tau && x - tau <= rc.highBound) {
          tau = vpKNNSearch(knns, rc);
        }
      }
      else {
        if(rc.lowBound <= x + tau && x - tau <= rc.highBound) {
          tau = vpKNNSearch(knns, rc);
        }
        if(lc.lowBound <= x + tau && x - tau <= lc.highBound) {
          tau = vpKNNSearch(knns, lc);
        }
      }
      return tau;
    }

    /**
     * Compute the distance to a candidate object.
     * 
     * @param p Object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef p);
  }

  /**
   * kNN search for the VP-Tree.
   *
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class VPTreeKNNObjectSearcher extends VPTreeKNNSearcher implements KNNSearcher<O> {
    /**
     * Current query object
     */
    private O query;

    @Override
    public KNNList getKNN(O query, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      this.query = query;
      vpKNNSearch(knns, root);
      return knns.toKNNList();
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return VPTree.this.distance(query, p);
    }
  }

  /**
   * kNN search for the VP-Tree.
   *
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class VPTreeKNNDBIDSearcher extends VPTreeKNNSearcher implements KNNSearcher<DBIDRef> {
    /**
     * Current query object
     */
    private DBIDRef query;

    @Override
    public KNNList getKNN(DBIDRef query, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      this.query = query;
      vpKNNSearch(knns, root);
      return knns.toKNNList();
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return VPTree.this.distance(query, p);
    }
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public static abstract class VPTreeRangeSearcher {
    /**
     * Recursive search function.
     *
     * @param result Result output
     * @param node Current node
     * @param range Search radius
     */
    protected void vpRangeSearch(ModifiableDoubleDBIDList result, Node node, double range) {
      final DoubleDBIDListMIter vp = node.vp.iter();
      final double x = queryDistance(vp);
      if(x <= range) {
        result.add(x, vp);
      }
      for(vp.advance(); vp.valid(); vp.advance()) {
        final double d = queryDistance(vp);
        if(d <= range) {
          result.add(d, vp);
        }
      }
      Node lc = node.leftChild, rc = node.rightChild;
      if(lc != null && lc.lowBound <= x + range && x - range <= lc.highBound) {
        vpRangeSearch(result, lc, range);
      }
      if(rc != null && rc.lowBound <= x + range && x - range <= rc.highBound) {
        vpRangeSearch(result, rc, range);
      }
    }

    /**
     * Compute the distance to a candidate object.
     * 
     * @param p Object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef p);
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class VPTreeRangeObjectSearcher extends VPTreeRangeSearcher implements RangeSearcher<O> {
    /**
     * Current query object
     */
    private O query;

    @Override
    public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      vpRangeSearch(result, root, range);
      return result;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return VPTree.this.distance(query, p);
    }
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class VPTreeRangeDBIDSearcher extends VPTreeRangeSearcher implements RangeSearcher<DBIDRef> {
    /**
     * Current query object
     */
    private DBIDRef query;

    @Override
    public ModifiableDoubleDBIDList getRange(DBIDRef query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      vpRangeSearch(result, root, range);
      return result;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return VPTree.this.distance(query, p);
    }
  }

  /**
   * Priority search for the VP-Tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   *
   * @param <Q> query type
   */
  public abstract class VPTreePrioritySearcher<Q> implements PrioritySearcher<Q> {
    /**
     * Min heap for searching.
     */
    private DoubleObjectMinHeap<Node> heap = new DoubleObjectMinHeap<>();

    /**
     * Stopping threshold.
     */
    private double threshold = Double.POSITIVE_INFINITY;

    /**
     * Lower bound
     */
    private double skipThreshold = 0.;

    /**
     * Current search position.
     */
    private Node cur;

    /**
     * Current iterator.
     */
    private DoubleDBIDListIter candidates = DoubleDBIDListIter.EMPTY;

    /**
     * Distance to the current object.
     */
    private double curdist, vpdist;

    /**
     * Start the search.
     */
    protected void reinit() {
      this.threshold = Double.POSITIVE_INFINITY;
      this.skipThreshold = 0;
      this.candidates = null;
      this.cur = null;
      this.curdist = Double.NaN;
      this.vpdist = Double.NaN;
      this.heap.clear();
      this.heap.add(0, root);
    }

    @Override
    public PrioritySearcher<Q> advance() {
      // Advance the main iterator, if defined:
      if(candidates.valid()) {
        candidates.advance();
      }
      // Additional points stored in the node / leaf
      do {
        while(candidates.valid()) {
          if(vpdist - candidates.doubleValue() <= threshold) {
            if(skipThreshold == 0 || vpdist + candidates.doubleValue() >= skipThreshold) {
              return this;
            }
          }
          candidates.advance();
        }
      }
      while(advanceQueue()); // Try next node
      return this;
    }

    /**
     * Expand the next node of the priority heap.
     * 
     * @return success
     */
    protected boolean advanceQueue() {
      if(heap.isEmpty()) {
        candidates = DoubleDBIDListIter.EMPTY;
        return false;
      }
      curdist = heap.peekKey();
      if(curdist > threshold) {
        heap.clear();
        candidates = DoubleDBIDListIter.EMPTY;
        return false;
      }
      cur = heap.peekValue();
      heap.poll(); // Remove
      candidates = cur.vp.iter();
      // Exact distance to vantage point:
      vpdist = queryDistance(candidates);

      Node lc = cur.leftChild, rc = cur.rightChild;
      // Add left subtree to heap
      if(lc != null) {
        final double mindist = Math.max(Math.max(vpdist - lc.highBound, lc.lowBound - vpdist), curdist);
        if(mindist <= threshold) {
          if(skipThreshold == 0 || vpdist + lc.highBound >= skipThreshold) {
            heap.add(mindist, lc);
          }
        }
      }
      // Add right subtree to heap
      if(rc != null) {
        final double mindist = Math.max(Math.max(vpdist - rc.highBound, rc.lowBound - vpdist), curdist);
        if(mindist <= threshold) {
          if(skipThreshold == 0 || vpdist + rc.highBound >= skipThreshold) {
            heap.add(mindist, rc);
          }
        }
      }
      return true;
    }

    /**
     * Compute the distance to a candidate object.
     * 
     * @param p Object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef p);

    @Override
    public int internalGetIndex() {
      return candidates.internalGetIndex();
    }

    @Override
    public boolean valid() {
      if(candidates == null) {
        candidates = DoubleDBIDListIter.EMPTY;
        advance();
      }
      return candidates.valid();
    }

    @Override
    public PrioritySearcher<Q> decreaseCutoff(double threshold) {
      assert threshold <= this.threshold : "Thresholds must only decrease: " + threshold + " > " + this.threshold;
      this.threshold = threshold;
      if(threshold < curdist) { // No more results possible:
        heap.clear();
        candidates = DoubleDBIDListIter.EMPTY;
      }
      return this;
    }

    @Override
    public PrioritySearcher<Q> increaseSkip(double threshold) {
      assert threshold >= this.skipThreshold : "Skip thresholds must only increase.";
      this.skipThreshold = threshold;
      return this;
    }

    @Override
    public double computeExactDistance() {
      return candidates.doubleValue() == 0. ? vpdist : queryDistance(candidates);
    }

    @Override
    public double getApproximateDistance() {
      return vpdist;
    }

    @Override
    public double getApproximateAccuracy() {
      return candidates.doubleValue();
    }

    @Override
    public double getLowerBound() {
      return Math.max(vpdist - candidates.doubleValue(), curdist);
    }

    @Override
    public double getUpperBound() {
      return vpdist + candidates.doubleValue();
    }

    @Override
    public double allLowerBound() {
      return curdist;
    }
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class VPTreePriorityObjectSearcher extends VPTreePrioritySearcher<O> {
    /**
     * Current query object
     */
    private O query;

    @Override
    public PrioritySearcher<O> search(O query) {
      this.query = query;
      reinit();
      return this;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return VPTree.this.distance(query, p);
    }
  }

  /**
   * Range search for the VP-tree.
   * 
   * @author Robert Gehde
   * @author Erich Schubert
   */
  public class VPTreePriorityDBIDSearcher extends VPTreePrioritySearcher<DBIDRef> {
    /**
     * Current query object
     */
    private DBIDRef query;

    @Override
    public PrioritySearcher<DBIDRef> search(DBIDRef query) {
      this.query = query;
      reinit();
      return this;
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return VPTree.this.distance(query, p);
    }
  }

  @Override
  public void logStatistics() {
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".distance-computations", distComputations));
  }

  /**
   * Index factory for the VP-Tree
   *
   * @author Robert Gehde
   *
   * @param <O> Object type
   */
  @Alias({ "vp" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O> {
    /**
     * Distance Function
     */
    Distance<? super O> distance;

    /**
     * Random factory
     */
    RandomFactory random;

    /**
     * Sample size
     */
    int sampleSize;

    /**
     * Truncation parameter
     */
    int truncate;

    /**
     * Constructor.
     * 
     * @param distFunc distance function
     * @param random random generator
     * @param sampleSize sample size
     * @param truncate maximum leaf size (truncation)
     */
    public Factory(Distance<? super O> distFunc, RandomFactory random, int sampleSize, int truncate) {
      super();
      this.distance = distFunc;
      this.random = random;
      this.sampleSize = Math.max(sampleSize, 1);
      this.truncate = Math.max(truncate, 1);
    }

    @Override
    public VPTree<O> instantiate(Relation<O> relation) {
      return new VPTree<>(relation, distance, random, sampleSize, truncate);
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
       * Parameter to specify the minimum leaf size
       */
      public final static OptionID TRUNCATE_ID = new OptionID("vptree.truncate", "Minimum leaf size for stopping.");

      /**
       * Parameter to specify the rnd generator seed
       */
      public final static OptionID SEED_ID = new OptionID("vptree.seed", "The rnd number generator seed.");

      /**
       * Distance function
       */
      protected Distance<? super O> distance;

      /**
       * Random generator
       */
      protected RandomFactory random;

      /**
       * Sample size
       */
      protected int sampleSize;

      /**
       * Truncation parameter
       */
      int truncate;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<? super O>>(DISTANCE_FUNCTION_ID, Distance.class) //
            .grab(config, x -> {
              this.distance = x;
              if(!distance.isMetric()) {
                LOG.warning("VPTree requires a metric to be exact.");
              }
            });
        new IntParameter(SAMPLE_SIZE_ID, 10) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> this.sampleSize = x);
        new IntParameter(TRUNCATE_ID, 8) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> this.truncate = x);
        new RandomParameter(SEED_ID).grab(config, x -> random = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(distance, random, sampleSize, truncate);
      }
    }
  }
}
