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
package elki.index.tree.spatial.kd;

import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.type.SimpleTypeInformation;
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
import elki.index.tree.spatial.kd.split.BoundedMidpointSplit;
import elki.index.tree.spatial.kd.split.SplitStrategy;
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

/**
 * Implementation of a static in-memory K-D-tree.
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
public class MemoryKDTree<O extends NumberVector> implements DistancePriorityIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MemoryKDTree.class);

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * Split stragegy
   */
  protected SplitStrategy split;

  /**
   * Maximum size of leaf nodes.
   */
  protected int leafsize;

  /**
   * The actual "tree" as a sorted array.
   */
  protected ArrayDBIDs sorted = null;

  /**
   * Root node
   */
  protected KDNode root;

  /**
   * The number of dimensions.
   */
  protected int dims = -1;

  /**
   * Counter for comparisons.
   */
  protected final Counter objaccess;

  /**
   * Counter for distance computations.
   */
  protected final Counter distcalc;

  /**
   * Constructor with default split (used by EmpiricalQueryOptimizer).
   *
   * @param relation Relation to index
   * @param leafsize Leaf size
   */
  public MemoryKDTree(Relation<O> relation, int leafsize) {
    this(relation, BoundedMidpointSplit.STATIC, leafsize);
  }

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param split Split strategy
   * @param leafsize Maximum size of leaf nodes
   */
  public MemoryKDTree(Relation<O> relation, SplitStrategy split, int leafsize) {
    this.relation = relation;
    this.split = split;
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
    dims = RelationUtil.dimensionality(relation);
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    // to count object accesses:
    Relation<O> rel = LOG.isStatistics() ? new CountingRelation() : relation;
    root = buildTree(rel, 0, ids.size(), ids, ids.iter(), new VectorUtil.SortDBIDsBySingleDimension(rel));
    sorted = ids;
  }

  /**
   * Proxy to count accesses.
   *
   * @author Erich Schubert
   */
  private class CountingRelation implements Relation<O> {
    @Override
    public O get(DBIDRef id) {
      countObjectAccess();
      return relation.get(id);
    }

    @Override
    public SimpleTypeInformation<O> getDataTypeInformation() {
      return relation.getDataTypeInformation();
    }

    @Override
    public DBIDs getDBIDs() {
      return relation.getDBIDs();
    }

    @Override
    public DBIDIter iterDBIDs() {
      return relation.iterDBIDs();
    }

    @Override
    public int size() {
      return relation.size();
    }

    @Override
    public String getLongName() {
      return relation.getLongName();
    }
  }

  /**
   * Build the k-d tree.
   *
   * @param relation Relation
   * @param left interval start
   * @param right interval end
   * @param sorted object ids
   * @param iter iterator on the ids
   * @param comp comparator on the values
   * @return root node
   */
  public KDNode buildTree(Relation<? extends NumberVector> relation, int left, int right, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, VectorUtil.SortDBIDsBySingleDimension comp) {
    if(right - left <= leafsize) {
      return new KDNode(left, right);
    }
    SplitStrategy.Info s = split.findSplit(relation, dims, sorted, iter, left, right, comp);
    if(s == null || s.pos >= right) {
      return new KDNode(left, right);
    }
    KDNode node = new KDNode(left, right, s.dim, s.val, buildTree(relation, left, s.pos, sorted, iter, comp), buildTree(relation, s.pos, right, sorted, iter, comp));
    assert assertSplitConsistent(left, s.pos, right, s.dim, s.val, iter);
    return node;
  }

  /**
   * KD tree node.
   *
   * @author Erich Schubert
   */
  public static class KDNode {
    /**
     * Splitting threshold
     */
    double split;

    /**
     * Split dimension
     */
    int dim;

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
     * @param start Start index of subtree range
     * @param end End index of subtree range
     */
    public KDNode(int start, int end) {
      this(start, end, -1, Double.NaN, null, null);
    }

    /**
     * Constructor.
     *
     * @param start Start index of subtree range
     * @param end End index of subtree range
     * @param dim Split dimension
     * @param split Split value
     * @param leftChild Left child
     * @param rightChild Right child
     */
    public KDNode(int start, int end, int dim, double split, KDNode leftChild, KDNode rightChild) {
      this.start = start;
      this.end = end;
      this.dim = dim;
      this.split = split;
      this.leftChild = leftChild;
      this.rightChild = rightChild;
    }
  }

  /**
   * Assert that the generated split is consistent.
   *
   * @param left Interval start
   * @param right Interval end
   * @param s Split information
   * @param iter Iterator
   */
  private boolean assertSplitConsistent(int left, int pos, int right, int dim, double val, DBIDArrayMIter iter) {
    for(iter.seek(left); iter.getOffset() < pos; iter.advance()) {
      assert relation.get(iter).doubleValue(dim) <= val : relation.get(iter).doubleValue(dim) + " > " + val + " at " + iter.getOffset() + "<" + pos;
    }
    for(iter.seek(pos); iter.getOffset() < right; iter.advance()) {
      assert relation.get(iter).doubleValue(dim) >= val : relation.get(iter).doubleValue(dim) + " < " + val + " at " + iter.getOffset() + ">=" + pos;
    }
    return true;
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
    if((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
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
    if((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
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
    if((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
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
      kdKNNSearch(root, obj, knns, sorted.iter(), new double[dims], 0, Double.POSITIVE_INFINITY);
      return knns.toKNNList();
    }

    /**
     * Perform a kNN search on the k-d-tree.
     *
     * @param node Current node
     * @param query Query object
     * @param knns kNN heap
     * @param iter Iterator variable (reduces memory footprint!)
     * @param bounds current bounds
     * @param rawdist Raw distance to current rectangle (usually squared)
     * @param maxdist Current upper bound of kNN distance.
     * @return New upper bound of kNN distance.
     */
    private double kdKNNSearch(KDNode node, O query, KNNHeap knns, DBIDArrayIter iter, double[] bounds, double rawdist, double maxdist) {
      final int axis = node.dim;
      if(axis == -1) { // leaf
        assert node.leftChild == null && node.rightChild == null;
        for(iter.seek(node.start); iter.getOffset() < node.end; iter.advance()) {
          double dist = distance.distance(query, relation.get(iter));
          // assert distance.compareRawRegular(rawdist, dist);
          countObjectAccess();
          countDistanceComputation();
          if(dist <= maxdist) {
            knns.insert(dist, iter);
            maxdist = knns.getKNNDistance();
          }
        }
        return maxdist;
      }
      assert node.leftChild != null && node.rightChild != null;
      // Distance to axis:
      final double delta = node.split - query.doubleValue(axis);
      if(delta == 0.0) {
        maxdist = kdKNNSearch(node.leftChild, query, knns, iter, bounds, rawdist, maxdist);
        maxdist = kdKNNSearch(node.rightChild, query, knns, iter, bounds, rawdist, maxdist);
      }
      else if(delta > 0) { // left first
        maxdist = kdKNNSearch(node.leftChild, query, knns, iter, bounds, rawdist, maxdist);
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, maxdist)) {
          bounds[axis] = delta;
          maxdist = kdKNNSearch(node.rightChild, query, knns, iter, bounds, mindist, maxdist);
          bounds[axis] = prevdelta; // restore
        }
      }
      else { // delta < 0, right first
        maxdist = kdKNNSearch(node.rightChild, query, knns, iter, bounds, rawdist, maxdist);
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, maxdist)) {
          bounds[axis] = delta;
          maxdist = kdKNNSearch(node.leftChild, query, knns, iter, bounds, mindist, maxdist);
          bounds[axis] = prevdelta; // restore
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
      double[] bounds = new double[dims];
      kdRangeSearch(root, obj, result, sorted.iter(), bounds, 0, range);
      return result;
    }

    /**
     * Perform a range search on the k-d-tree.
     *
     * @param node Current node
     * @param query Query object
     * @param res kNN heap
     * @param iter Iterator variable (reduces memory footprint!)
     * @param rawdist Raw distance to current rectangle (usually squared)
     * @param radius Query radius
     */
    private void kdRangeSearch(KDNode node, O query, ModifiableDoubleDBIDList res, DBIDArrayIter iter, double[] bounds, double rawdist, double radius) {
      final int axis = node.dim;
      if(axis == -1) {
        for(iter.seek(node.start); iter.getOffset() < node.end; iter.advance()) {
          double dist = distance.distance(query, relation.get(iter));
          // assert distance.compareRawRegular(rawdist, dist);
          countObjectAccess();
          countDistanceComputation();
          if(dist <= radius) {
            res.add(dist, iter);
          }
        }
        return;
      }
      // Distance to axis:
      final double delta = node.split - query.doubleValue(axis);
      if(delta == 0) {
        kdRangeSearch(node.leftChild, query, res, iter, bounds, rawdist, radius);
        kdRangeSearch(node.rightChild, query, res, iter, bounds, rawdist, radius);
      }
      else if(delta > 0) {
        kdRangeSearch(node.leftChild, query, res, iter, bounds, rawdist, radius);
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, radius)) {
          bounds[axis] = delta;
          kdRangeSearch(node.rightChild, query, res, iter, bounds, mindist, radius);
          bounds[axis] = prevdelta; // restore
        }
      }
      else {
        final double prevdelta = bounds[axis];
        final double mindist = distance.combineRaw(rawdist, delta, prevdelta);
        if(distance.compareRawRegular(mindist, radius)) {
          bounds[axis] = delta;
          kdRangeSearch(node.leftChild, query, res, iter, bounds, mindist, radius);
          bounds[axis] = prevdelta; // restore
        }
        kdRangeSearch(node.rightChild, query, res, iter, bounds, rawdist, radius);
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
     * Current node
     */
    KDNode node;

    /**
     * Constructor.
     *
     * @param mindist Minimum distance
     * @param node Tree node
     */
    public PrioritySearchBranch(double mindist, KDNode node) {
      this.mindist = mindist;
      this.node = node;
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
    private DBIDArrayIter iter = sorted.iter();

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
      this.heap.add(new PrioritySearchBranch(0, root));
      return advance();
    }

    @Override
    public PrioritySearcher<O> advance() {
      while(true) {
        // Iteration within current leaf:
        if(cur != null && cur.node.dim == -1) {
          assert pos >= cur.node.start;
          if(++pos < cur.node.end) {
            return this;
          }
          assert pos == cur.node.end;
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
        final int axis = cur.node.dim;
        // Reached leaf:
        if(axis == -1) {
          pos = cur.node.start;
          return this;
        }
        // Distance to axis:
        final double delta = cur.node.split - query.doubleValue(axis);
        final double mindist = distance instanceof SquaredEuclideanDistance ? delta * delta : Math.abs(delta);

        final double ldist = delta < 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
        if(ldist <= threshold) {
          heap.add(new PrioritySearchBranch(ldist, cur.node.leftChild));
        }
        final double rdist = delta > 0 ? Math.max(mindist, cur.mindist) : cur.mindist;
        if(rdist <= threshold) {
          heap.add(new PrioritySearchBranch(rdist, cur.node.rightChild));
        }
      }
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
  @Alias({ "kd" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O> {
    /**
     * Split stragegy
     */
    SplitStrategy split;

    /**
     * Maximum size of leaf nodes.
     */
    int leafsize;

    /**
     * Constructor.
     *
     * @param split Split strategy
     * @param leafsize Maximum size of leaf nodes.
     */
    public Factory(SplitStrategy split, int leafsize) {
      super();
      this.split = split;
      this.leafsize = leafsize;
    }

    @Override
    public MemoryKDTree<O> instantiate(Relation<O> relation) {
      return new MemoryKDTree<>(relation, split, leafsize);
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
      public static final OptionID SPLIT_P = new OptionID("kd.split", "Split strategy for the k-d-tree.");

      /**
       * Option for setting the maximum leaf size.
       */
      public static final OptionID LEAFSIZE_P = MinimalisticMemoryKDTree.Factory.Par.LEAFSIZE_P;

      /**
       * Split stragegy
       */
      SplitStrategy split;

      /**
       * Maximum size of leaf nodes.
       */
      int leafsize;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<SplitStrategy>(SPLIT_P, SplitStrategy.class, BoundedMidpointSplit.class) //
            .grab(config, x -> split = x);
        new IntParameter(LEAFSIZE_P, 2) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> leafsize = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(split, leafsize);
      }
    }
  }
}
