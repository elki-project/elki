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
package elki.index.tree.spatial.kd;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.*;
import elki.index.DistancePriorityIndex;
import elki.index.IndexFactory;
import elki.logging.Logging;
import elki.logging.statistics.Counter;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.datastructures.heap.ComparableMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Simple implementation of a static in-memory K-D-tree. Does not support
 * dynamic updates or anything, but also is very simple and memory efficient:
 * all it uses is one {@link ModifiableDoubleDBIDList} to sort the data in a
 * serialized tree and store the current attribute value.
 * <p>
 * It needs about 3 times as much memory as {@link MinimalisticMemoryKDTree} but
 * it is also considerably faster because it does not need to lookup this value
 * from the vectors. {@link MemoryKDTree} needs even more memory, but uses much
 * better splits and hence is usually the best choice.
 * <p>
 * Reference:
 * <p>
 * J. L. Bentley<br>
 * Multidimensional binary search trees used for associative searching<br>
 * Communications of the ACM 18(9)
 * <p>
 * The search uses an improved search strategy published by:
 * <p>
 * S. Arya and D. M. Mount<br>
 * Algorithms for fast vector quantization<br>
 * Proc. DCC '93: Data Compression Conference
 * <p>
 * TODO: add support for weighted Minkowski distances.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KDTreeKNNSearcher
 * @has - - - KDTreeRangeSearcher
 *
 * @param <O> Vector type
 */
@Reference(authors = "J. L. Bentley", //
    title = "Multidimensional binary search trees used for associative searching", //
    booktitle = "Communications of the ACM 18(9)", //
    url = "https://doi.org/10.1145/361002.361007", //
    bibkey = "DBLP:journals/cacm/Bentley75")
@Priority(Priority.SUPPLEMENTARY - 1)
public class SmallMemoryKDTree<O extends NumberVector> implements DistancePriorityIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SmallMemoryKDTree.class);

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * The actual "tree" as a sorted array.
   */
  protected ModifiableDoubleDBIDList sorted = null;

  /**
   * The number of dimensions.
   */
  protected int dims = -1;

  /**
   * Maximum size of leaf nodes.
   */
  protected int leafsize;

  /**
   * Counter for comparisons.
   */
  protected final Counter objaccess;

  /**
   * Counter for distance computations.
   */
  protected final Counter distcalc;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param leafsize Maximum size of leaf nodes
   */
  public SmallMemoryKDTree(Relation<O> relation, int leafsize) {
    this.relation = relation;
    this.leafsize = leafsize;
    assert (leafsize >= 1);
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
    sorted = DBIDUtil.newDistanceDBIDList(relation.size());
    dims = RelationUtil.dimensionality(relation);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      sorted.add(Double.NaN, it);
    }
    buildTree(0, sorted.size(), 0, sorted.iter());
  }

  /**
   * Recursively build the tree by partial sorting. O(n log n) complexity.
   * Apparently there exists a variant in only O(n log log n)? Please
   * contribute!
   *
   * @param left Interval minimum
   * @param right Interval maximum
   * @param axis Current splitting axis
   * @param iter Iterator
   */
  private void buildTree(int left, int right, int axis, DoubleDBIDListMIter iter) {
    assert (left < right);
    for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
      iter.setDouble(relation.get(iter).doubleValue(axis));
      countObjectAccess();
    }
    if(right - left <= leafsize) {
      return;
    }

    int middle = (left + right) >>> 1;
    QuickSelectDBIDs.quickSelect(sorted, left, right, middle);
    final int next = next(axis);
    if(left < middle) {
      buildTree(left, middle, next, iter);
    }
    ++middle;
    if(middle < right) {
      buildTree(middle, right, next, iter);
    }
  }

  /**
   * Next axis.
   *
   * @param axis Current axis
   * @return Next axis
   */
  private int next(int axis) {
    return ++axis == dims ? 0 : axis;
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
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if ((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      return null; // Precomputed only requested
    }
    Distance<? super O> df = distanceQuery.getDistance();
    if(df instanceof SquaredEuclideanDistance) {
      return new KDTreeKNNSearcher(PartialSquaredEuclideanDistance.STATIC);
    }
    if(df instanceof EuclideanDistance) {
      return new KDTreeKNNSearcher(PartialEuclideanDistance.STATIC);
    }
    if(df instanceof ManhattanDistance) {
      return new KDTreeKNNSearcher(PartialManhattanDistance.STATIC);
    }
    if(df instanceof LPNormDistance) {
      return new KDTreeKNNSearcher(new PartialLPNormDistance((LPNormDistance) df));
    }
    // TODO: if we know this works for other distance functions, add them, too!
    return null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    if ((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      return null; // Precomputed only requested
    }
    Distance<? super O> df = distanceQuery.getDistance();
    if(df instanceof SquaredEuclideanDistance) {
      return new KDTreeRangeSearcher(PartialSquaredEuclideanDistance.STATIC);
    }
    if(df instanceof EuclideanDistance) {
      return new KDTreeRangeSearcher(PartialEuclideanDistance.STATIC);
    }
    if(df instanceof ManhattanDistance) {
      return new KDTreeRangeSearcher(PartialManhattanDistance.STATIC);
    }
    if(df instanceof LPNormDistance) {
      return new KDTreeRangeSearcher(new PartialLPNormDistance((LPNormDistance) df));
    }
    // TODO: if we know this works for other distance functions, add them, too!
    return null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    if ((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      return null; // Precomputed only requested
    }
    Distance<? super O> df = distanceQuery.getDistance();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistance || df instanceof SquaredEuclideanDistance //
        || df instanceof SparseLPNormDistance) {
      return new KDTreePrioritySearcher((PrimitiveDistance<? super O>) df);
    }
    return null;
  }

  /**
   * kNN query for the k-d-tree.
   * <p>
   * Reference:
   * <p>
   * S. Arya and D. M. Mount<br>
   * Algorithms for fast vector quantization<br>
   * Proc. DCC '93: Data Compression Conference
   *
   * @author Erich Schubert
   */
  @Reference(authors = "S. Arya and D. M. Mount", //
      title = "Algorithms for fast vector quantization", //
      booktitle = "Proc. DCC '93: Data Compression Conference", //
      url = "https://doi.org/10.1109/DCC.1993.253111", //
      bibkey = "doi:10.1109/DCC.1993.253111")
  public class KDTreeKNNSearcher implements KNNSearcher<O> {
    /**
     * Distance to use.
     */
    private PartialDistance<? super O> distance;

    /**
     * Constructor.
     *
     * @param distance Distance to use
     */
    public KDTreeKNNSearcher(PartialDistance<? super O> distance) {
      super();
      this.distance = distance;
    }

    @Override
    public KNNList getKNN(O obj, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      kdKNNSearch(0, sorted.size(), 0, obj, knns, sorted.iter(), new double[dims], 0, Double.POSITIVE_INFINITY);
      return knns.toKNNList();
    }

    /**
     * Perform a kNN search on the k-d-tree.
     *
     * @param left Subtree begin
     * @param right Subtree end (exclusive)
     * @param axis Current splitting axis
     * @param query Query object
     * @param knns kNN heap
     * @param iter Iterator variable (reduces memory footprint!)
     * @param bounds current bounds
     * @param rawdist Raw distance to current rectangle (usually squared)
     * @param maxdist Current upper bound of kNN distance.
     * @return New upper bound of kNN distance.
     */
    private double kdKNNSearch(int left, int right, int axis, O query, KNNHeap knns, DoubleDBIDListIter iter, double[] bounds, double rawdist, double maxdist) {
      if(right - left <= leafsize) {
        for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
          double dist = distance.distance(query, relation.get(iter));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= maxdist) {
            knns.insert(dist, iter);
            maxdist = knns.getKNNDistance();
            if(distance.compareRawRegular(rawdist, maxdist)) {
              break;
            }
          }
        }
        return maxdist;
      }
      // Look at current node:
      final int middle = (left + right) >>> 1;
      // Distance to axis:
      final double delta = iter.seek(middle).doubleValue() - query.doubleValue(axis);
      assert (iter.doubleValue() == relation.get(iter).doubleValue(axis)) : "Tree inconsistent " + left + " < " + middle + " < " + right + ": " + iter.doubleValue() + " != " + relation.get(iter).doubleValue(axis) + " " + relation.get(iter);
      final int next = next(axis);

      // Exact match chance (delta == 0)!
      // process split object first, then descend both sides.
      if(delta == 0.0) {
        double dist = distance.distance(query, relation.get(iter.seek(middle)));
        countObjectAccess();
        countDistanceComputation();
        if(dist <= maxdist) {
          assert iter.getOffset() == middle;
          knns.insert(dist, iter /* actually: iter.seek(middle) */);
          maxdist = knns.getKNNDistance();
        }
        if(left < middle) {
          maxdist = kdKNNSearch(left, middle, next, query, knns, iter, bounds, rawdist, maxdist);
        }
        if(middle + 1 < right) {
          maxdist = kdKNNSearch(middle + 1, right, next, query, knns, iter, bounds, rawdist, maxdist);
        }
      }
      else if(delta > 0) { // left first
        if(left < middle) {
          maxdist = kdKNNSearch(left, middle, next, query, knns, iter, bounds, rawdist, maxdist);
        }
        // Look at splitting element (unless already above):
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, maxdist)) {
          double dist = distance.distance(query, relation.get(iter.seek(middle)));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= maxdist) {
            knns.insert(dist, iter.seek(middle));
            maxdist = knns.getKNNDistance();
          }
          if(middle + 1 < right) {
            bounds[axis] = delta;
            maxdist = kdKNNSearch(middle + 1, right, next, query, knns, iter, bounds, mindist, maxdist);
            bounds[axis] = prevdelta; // restore
          }
        }
      }
      else { // delta > 0, right first
        if(middle + 1 < right) {
          maxdist = kdKNNSearch(middle + 1, right, next, query, knns, iter, bounds, rawdist, maxdist);
        }
        // Look at splitting element (unless already above):
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, maxdist)) {
          double dist = distance.distance(query, relation.get(iter.seek(middle)));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= maxdist) {
            knns.insert(dist, iter.seek(middle));
            maxdist = knns.getKNNDistance();
          }
          if(left < middle) {
            bounds[axis] = delta;
            maxdist = kdKNNSearch(left, middle, next, query, knns, iter, bounds, mindist, maxdist);
            bounds[axis] = prevdelta; // restore
          }
        }
      }
      return maxdist;
    }
  }

  /**
   * Range query for the k-d-tree.
   * <p>
   * Reference:
   * <p>
   * S. Arya and D. M. Mount<br>
   * Algorithms for fast vector quantization<br>
   * Proc. DCC '93: Data Compression Conference
   *
   * @author Erich Schubert
   */
  @Reference(authors = "S. Arya and D. M. Mount", //
      title = "Algorithms for fast vector quantization", //
      booktitle = "Proc. DCC '93: Data Compression Conference", //
      url = "https://doi.org/10.1109/DCC.1993.253111", //
      bibkey = "doi:10.1109/DCC.1993.253111")
  public class KDTreeRangeSearcher implements RangeSearcher<O> {
    /**
     * Distance to use.
     */
    private PartialDistance<? super O> distance;

    /**
     * Constructor.
     *
     * @param distance Distance to use
     */
    public KDTreeRangeSearcher(PartialDistance<? super O> distance) {
      super();
      this.distance = distance;
    }

    @Override
    public ModifiableDoubleDBIDList getRange(O obj, double range, ModifiableDoubleDBIDList result) {
      kdRangeSearch(0, sorted.size(), 0, obj, result, sorted.iter(), new double[dims], 0, range);
      return result;
    }

    /**
     * Perform a range search on the k-d-tree.
     *
     * @param left Subtree begin
     * @param right Subtree end (exclusive)
     * @param axis Current splitting axis
     * @param query Query object
     * @param res kNN heap
     * @param iter Iterator variable (reduces memory footprint!)
     * @param rawdist Raw distance to current rectangle (usually squared)
     * @param radius Query radius
     */
    private void kdRangeSearch(int left, int right, int axis, O query, ModifiableDoubleDBIDList res, DoubleDBIDListIter iter, double[] bounds, double rawdist, double radius) {
      if(right - left <= leafsize) {
        for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
          double dist = distance.distance(query, relation.get(iter));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= radius) {
            res.add(dist, iter);
          }
        }
        return;
      }
      // Look at current node:
      final int middle = (left + right) >>> 1;
      // Distance to axis:
      final double delta = iter.seek(middle).doubleValue() - query.doubleValue(axis);
      final int next = next(axis);

      if(delta == 0) {
        // Current object:
        double dist = distance.distance(query, relation.get(iter.seek(middle)));
        countObjectAccess();
        countDistanceComputation();
        if(dist <= radius) {
          assert (iter.getOffset() == middle);
          res.add(dist, iter /* .seek(middle) */);
        }
        if(left < middle) {
          kdRangeSearch(left, middle, next, query, res, iter, bounds, rawdist, radius);
        }
        if(middle + 1 < right) {
          kdRangeSearch(middle + 1, right, next, query, res, iter, bounds, rawdist, radius);
        }
      }
      else if(delta > 0) {
        if(left < middle) {
          kdRangeSearch(left, middle, next, query, res, iter, bounds, rawdist, radius);
        }
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, radius)) {
          // Current object:
          double dist = distance.distance(query, relation.get(iter.seek(middle)));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= radius) {
            res.add(dist, iter.seek(middle));
          }
          if(middle + 1 < right) {
            bounds[axis] = delta;
            kdRangeSearch(middle + 1, right, next, query, res, iter, bounds, mindist, radius);
            bounds[axis] = prevdelta; // restore
          }
        }
      }
      else {
        if(middle + 1 < right) {
          kdRangeSearch(middle + 1, right, next, query, res, iter, bounds, rawdist, radius);
        }
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, radius)) {
          // Current object:
          double dist = distance.distance(query, relation.get(iter.seek(middle)));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= radius) {
            res.add(dist, iter.seek(middle));
          }
          if(left < middle) {
            bounds[axis] = delta;
            kdRangeSearch(left, middle, next, query, res, iter, bounds, mindist, radius);
            bounds[axis] = prevdelta; // restore
          }
        }
      }
    }
  }

  /**
   * Search position for priority search.
   *
   * @author Erich Schubert
   */
  private static class PrioritySearchBranch implements Comparable<PrioritySearchBranch> {
    /**
     * Minimum distance
     */
    double mindist;

    /**
     * Interval begin
     */
    int left;

    /**
     * Interval end
     */
    int right;

    /**
     * Next splitting axis
     */
    int axis;

    /**
     * Constructor.
     *
     * @param mindist Minimum distance
     * @param left Interval begin
     * @param right Interval end (exclusive)
     * @param axis Next axis
     */
    public PrioritySearchBranch(double mindist, int left, int right, int axis) {
      this.mindist = mindist;
      this.left = left;
      this.right = right;
      this.axis = axis;
    }

    @Override
    public int compareTo(PrioritySearchBranch o) {
      return Double.compare(this.mindist, o.mindist);
    }
  }

  /**
   * Priority search for the k-d-tree.
   *
   * @author Erich Schubert
   */
  public class KDTreePrioritySearcher implements PrioritySearcher<O> {
    /**
     * Distance to use.
     */
    private PrimitiveDistance<? super O> distance;

    /**
     * Min heap for searching.
     */
    private ComparableMinHeap<PrioritySearchBranch> heap = new ComparableMinHeap<>();

    /**
     * Search iterator.
     */
    private DoubleDBIDListIter iter = sorted.iter();

    /**
     * Current query object.
     */
    private O query;

    /**
     * Stopping threshold.
     */
    private double threshold;

    /**
     * Position within leaf.
     */
    private int pos;

    /**
     * Current search position.
     */
    private PrioritySearchBranch cur;

    /**
     * Constructor.
     *
     * @param distance Distance to use
     */
    public KDTreePrioritySearcher(PrimitiveDistance<? super O> distance) {
      super();
      this.distance = distance;
    }

    @Override
    public PrioritySearcher<O> search(O query) {
      this.query = query;
      this.threshold = Double.POSITIVE_INFINITY;
      this.pos = Integer.MIN_VALUE;
      this.heap.clear();
      this.heap.add(new PrioritySearchBranch(0, 0, sorted.size(), 0));
      return advance();
    }

    @Override
    public PrioritySearcher<O> advance() {
      // Iteration within current leaf:
      if(cur != null && cur.right - cur.left <= leafsize) {
        assert pos >= cur.left;
        if(++pos < cur.right) {
          return this;
        }
        assert pos == cur.right;
      }
      if(heap.isEmpty()) {
        cur = null;
        pos = Integer.MIN_VALUE;
        return this;
      }
      // Get next
      cur = heap.poll();
      if(cur.mindist > threshold) {
        cur = null;
        pos = Integer.MIN_VALUE;
        return this;
      }
      // Leaf:
      if(cur.right - cur.left <= leafsize) {
        pos = cur.left;
        return this;
      }
      pos = (cur.left + cur.right) >>> 1; // middle element
      // Distance to axis:
      final double delta = iter.seek(pos).doubleValue() - query.doubleValue(cur.axis);
      final double mindist = distance instanceof SquaredEuclideanDistance ? delta * delta : Math.abs(delta);

      // Next axis:
      final int next = next(cur.axis);
      final double ldist = delta < 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
      if(cur.left < pos && ldist <= threshold) {
        heap.add(new PrioritySearchBranch(ldist, cur.left, pos, next));
      }
      final double rdist = delta > 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
      if(pos + 1 < cur.right && rdist <= threshold) {
        heap.add(new PrioritySearchBranch(rdist, pos + 1, cur.right, next));
      }
      return this;
    }

    @Override
    public boolean valid() {
      return pos >= 0;
    }

    @Override
    public double getLowerBound() {
      return cur.mindist;
    }

    @Override
    public double allLowerBound() {
      return cur.mindist;
    }

    @Override
    public double computeExactDistance() {
      countDistanceComputation();
      countObjectAccess();
      return distance.distance(query, relation.get(iter.seek(pos)));
    }

    @Override
    public int internalGetIndex() {
      return iter.seek(pos).internalGetIndex();
    }

    @Override
    public PrioritySearcher<O> decreaseCutoff(double threshold) {
      assert threshold <= this.threshold : "Thresholds must only decreasee.";
      this.threshold = threshold;
      return this;
    }
  }

  /**
   * Factory class
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @has - - - SmallMemoryKDTree
   *
   * @param <O> Vector type
   */
  @Alias({ "smallkd" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O> {
    /**
     * Maximum size of leaf nodes.
     */
    int leafsize;

    /**
     * Constructor.
     */
    public Factory() {
      this(1);
    }

    /**
     * Constructor.
     *
     * @param leafsize Maximum size of leaf nodes.
     */
    public Factory(int leafsize) {
      super();
      this.leafsize = leafsize;
    }

    @Override
    public SmallMemoryKDTree<O> instantiate(Relation<O> relation) {
      return new SmallMemoryKDTree<>(relation, leafsize);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O extends NumberVector> implements Parameterizer {
      /**
       * Option for setting the maximum leaf size.
       */
      public static final OptionID LEAFSIZE_P = MinimalisticMemoryKDTree.Factory.Par.LEAFSIZE_P;

      /**
       * Maximum size of leaf nodes.
       */
      int leafsize;

      @Override
      public void configure(Parameterization config) {
        new IntParameter(LEAFSIZE_P, 1) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> leafsize = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(leafsize);
      }
    }
  }
}
