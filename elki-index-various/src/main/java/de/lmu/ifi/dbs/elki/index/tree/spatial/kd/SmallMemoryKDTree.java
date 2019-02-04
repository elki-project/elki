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
package de.lmu.ifi.dbs.elki.index.tree.spatial.kd;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SparseLPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Simple implementation of a static in-memory K-D-tree. Does not support
 * dynamic updates or anything, but also is very simple and memory efficient:
 * all it uses is one {@link ModifiableDoubleDBIDList} to sort the data in a
 * serialized tree and store the current attribute value.
 * <p>
 * It needs about 3 times as much memory as {@link MinimalisticMemoryKDTree} but
 * it is also considerably faster because it does not need to lookup this value
 * from the vectors.
 * <p>
 * Reference:
 * <p>
 * J. L. Bentley<br>
 * Multidimensional binary search trees used for associative searching<br>
 * Communications of the ACM 18(9)
 * <p>
 * TODO: add support for weighted Minkowski distances.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KDTreeKNNQuery
 * @has - - - KDTreeRangeQuery
 *
 * @param <O> Vector type
 */
@Reference(authors = "J. L. Bentley", //
    title = "Multidimensional binary search trees used for associative searching", //
    booktitle = "Communications of the ACM 18(9)", //
    url = "https://doi.org/10.1145/361002.361007", //
    bibkey = "DBLP:journals/cacm/Bentley75")
public class SmallMemoryKDTree<O extends NumberVector> extends AbstractIndex<O> implements KNNIndex<O>, RangeIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SmallMemoryKDTree.class);

  /**
   * The actual "tree" as a sorted array.
   */
  ModifiableDoubleDBIDList sorted = null;

  /**
   * The number of dimensions.
   */
  int dims = -1;

  /**
   * Maximum size of leaf nodes.
   */
  int leafsize;

  /**
   * Counter for comparisons.
   */
  final Counter objaccess;

  /**
   * Counter for distance computations.
   */
  final Counter distcalc;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param leafsize Maximum size of leaf nodes
   */
  public SmallMemoryKDTree(Relation<O> relation, int leafsize) {
    super(relation);
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
    final int next = (axis + 1) % dims;
    if(left < middle) {
      buildTree(left, middle, next, iter);
    }
    ++middle;
    if(middle < right) {
      buildTree(middle, right, next, iter);
    }
  }

  @Override
  public String getLongName() {
    return "kd-tree";
  }

  @Override
  public String getShortName() {
    return "kd-tree";
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
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    DistanceFunction<? super O> df = distanceQuery.getDistanceFunction();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistanceFunction) {
      return new KDTreeKNNQuery(distanceQuery, (Norm<? super O>) df);
    }
    if(df instanceof SquaredEuclideanDistanceFunction) {
      return new KDTreeKNNQuery(distanceQuery, (Norm<? super O>) df);
    }
    if(df instanceof SparseLPNormDistanceFunction) {
      return new KDTreeKNNQuery(distanceQuery, (Norm<? super O>) df);
    }
    return null;
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    DistanceFunction<? super O> df = distanceQuery.getDistanceFunction();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistanceFunction) {
      return new KDTreeRangeQuery(distanceQuery, (Norm<? super O>) df);
    }
    if(df instanceof SquaredEuclideanDistanceFunction) {
      return new KDTreeRangeQuery(distanceQuery, (Norm<? super O>) df);
    }
    if(df instanceof SparseLPNormDistanceFunction) {
      return new KDTreeRangeQuery(distanceQuery, (Norm<? super O>) df);
    }
    return null;
  }

  /**
   * kNN query for the k-d-tree.
   *
   * @author Erich Schubert
   */
  public class KDTreeKNNQuery extends AbstractDistanceKNNQuery<O> {
    /**
     * Norm to use.
     */
    private Norm<? super O> norm;

    /**
     * Constructor.
     *
     * @param distanceQuery Distance query
     * @param norm Norm to use
     */
    public KDTreeKNNQuery(DistanceQuery<O> distanceQuery, Norm<? super O> norm) {
      super(distanceQuery);
      this.norm = norm;
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      kdKNNSearch(0, sorted.size(), 0, obj, knns, sorted.iter(), Double.POSITIVE_INFINITY);
      return knns.toKNNList();
    }

    /**
     * Perform a kNN search on the kd-tree.
     *
     * @param left Subtree begin
     * @param right Subtree end (exclusive)
     * @param axis Current splitting axis
     * @param query Query object
     * @param knns kNN heap
     * @param iter Iterator variable (reduces memory footprint!)
     * @param maxdist Current upper bound of kNN distance.
     * @return New upper bound of kNN distance.
     */
    private double kdKNNSearch(int left, int right, int axis, O query, KNNHeap knns, DoubleDBIDListIter iter, double maxdist) {
      if(right - left <= leafsize) {
        for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
          double dist = norm.distance(query, relation.get(iter));
          countObjectAccess();
          countDistanceComputation();
          if(dist <= maxdist) {
            knns.insert(dist, iter);
          }
          maxdist = knns.getKNNDistance();
        }
        return maxdist;
      }
      // Look at current node:
      final int middle = (left + right) >>> 1;

      // Distance to axis:
      final double delta = iter.seek(middle).doubleValue() - query.doubleValue(axis);
      assert (iter.doubleValue() == relation.get(iter).doubleValue(axis)) : "Tree inconsistent " + left + " < " + middle + " < " + right + ": " + iter.doubleValue() + " != " + relation.get(iter).doubleValue(axis) + " " + relation.get(iter);
      final boolean onleft = (delta >= 0);
      final boolean onright = (delta <= 0);

      // Next axis:
      final int next = (axis + 1) % dims;

      // Exact match chance (delta == 0)!
      // process first, then descend both sides.
      if(onleft && onright) {
        O split = relation.get(iter.seek(middle));
        countObjectAccess();
        double dist = norm.distance(query, split);
        countDistanceComputation();
        if(dist <= maxdist) {
          assert (iter.getOffset() == middle);
          knns.insert(dist, iter /* .seek(middle) */);
          maxdist = knns.getKNNDistance();
        }
        if(left < middle) {
          maxdist = kdKNNSearch(left, middle, next, query, knns, iter, maxdist);
        }
        if(middle + 1 < right) {
          maxdist = kdKNNSearch(middle + 1, right, next, query, knns, iter, maxdist);
        }
      }
      else {
        if(onleft) {
          if(left < middle) {
            maxdist = kdKNNSearch(left, middle, next, query, knns, iter, maxdist);
          }
          // Look at splitting element (unless already above):
          if(Math.abs(delta) <= maxdist) {
            O split = relation.get(iter.seek(middle));
            countObjectAccess();
            double dist = norm.distance(query, split);
            countDistanceComputation();
            if(dist <= maxdist) {
              assert (iter.getOffset() == middle);
              knns.insert(dist, iter /* .seek(middle) */);
              maxdist = knns.getKNNDistance();
            }
          }
          if((middle + 1 < right) && (Math.abs(delta) <= maxdist)) {
            maxdist = kdKNNSearch(middle + 1, right, next, query, knns, iter, maxdist);
          }
        }
        else { // onright
          if(middle + 1 < right) {
            maxdist = kdKNNSearch(middle + 1, right, next, query, knns, iter, maxdist);
          }
          // Look at splitting element (unless already above):
          if(Math.abs(delta) <= maxdist) {
            O split = relation.get(iter.seek(middle));
            countObjectAccess();
            double dist = norm.distance(query, split);
            countDistanceComputation();
            if(dist <= maxdist) {
              iter.seek(middle);
              knns.insert(dist, iter);
              maxdist = knns.getKNNDistance();
            }
          }
          if((left < middle) && (Math.abs(delta) <= maxdist)) {
            maxdist = kdKNNSearch(left, middle, next, query, knns, iter, maxdist);
          }
        }
      }
      return maxdist;
    }
  }

  /**
   * kNN query for the k-d-tree.
   *
   * @author Erich Schubert
   */
  public class KDTreeRangeQuery extends AbstractDistanceRangeQuery<O> {
    /**
     * Norm to use.
     */
    private Norm<? super O> norm;

    /**
     * Constructor.
     *
     * @param distanceQuery Distance query
     * @param norm Norm to use
     */
    public KDTreeRangeQuery(DistanceQuery<O> distanceQuery, Norm<? super O> norm) {
      super(distanceQuery);
      this.norm = norm;
    }

    @Override
    public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
      kdRangeSearch(0, sorted.size(), 0, obj, result, sorted.iter(), range);
    }

    /**
     * Perform a kNN search on the kd-tree.
     *
     * @param left Subtree begin
     * @param right Subtree end (exclusive)
     * @param axis Current splitting axis
     * @param query Query object
     * @param res kNN heap
     * @param iter Iterator variable (reduces memory footprint!)
     * @param radius Query radius
     */
    private void kdRangeSearch(int left, int right, int axis, O query, ModifiableDoubleDBIDList res, DoubleDBIDListIter iter, double radius) {
      if(right - left <= leafsize) {
        for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
          double dist = norm.distance(query, relation.get(iter));
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
      final boolean onleft = (delta >= 0);
      final boolean onright = (delta <= 0);
      final boolean close = (Math.abs(delta) <= radius);

      // Next axis:
      final int next = (axis + 1) % dims;

      // Current object:
      if(close) {
        O split = relation.get(iter.seek(middle));
        countObjectAccess();
        double dist = norm.distance(query, split);
        countDistanceComputation();
        if(dist <= radius) {
          assert (iter.getOffset() == middle);
          res.add(dist, iter /* .seek(middle) */);
        }
      }
      if(left < middle && (onleft || close)) {
        kdRangeSearch(left, middle, next, query, res, iter, radius);
      }
      if(middle + 1 < right && (onright || close)) {
        kdRangeSearch(middle + 1, right, next, query, res, iter, radius);
      }
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
  @Alias({ "smallkd", "kd" })
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
    public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
      /**
       * Maximum size of leaf nodes.
       */
      int leafsize;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        IntParameter leafP = new IntParameter(MinimalisticMemoryKDTree.Factory.Parameterizer.LEAFSIZE_P, 1) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(leafP)) {
          leafsize = leafP.intValue();
        }
      }

      @Override
      protected Factory<O> makeInstance() {
        return new Factory<>(leafsize);
      }
    }
  }
}
