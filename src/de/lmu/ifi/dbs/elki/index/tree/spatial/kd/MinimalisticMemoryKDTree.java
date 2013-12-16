package de.lmu.ifi.dbs.elki.index.tree.spatial.kd;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPairList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DoubleNorm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SparseLPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Simple implementation of a static in-memory K-D-tree. Does not support
 * dynamic updates or anything, but also is very simple and memory efficient:
 * all it uses is one {@link ArrayModifiableDBIDs} to sort the data in a
 * serialized tree.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KDTreeKNNQuery
 * @apiviz.has KDTreeRangeQuery
 * 
 * @param <O> Vector type
 */
@Reference(authors = "J. L. Bentley", title = "Multidimensional binary search trees used for associative searching", booktitle = "Communications of the ACM, Vol. 18 Issue 9, Sept. 1975", url = "http://dx.doi.org/10.1145/361002.361007")
public class MinimalisticMemoryKDTree<O extends NumberVector<?>> extends AbstractIndex<O> implements KNNIndex<O>, RangeIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MinimalisticMemoryKDTree.class);

  /**
   * The actual "tree" as a sorted array.
   */
  ArrayModifiableDBIDs sorted = null;

  /**
   * The number of dimensions.
   */
  int dims = -1;

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
   */
  public MinimalisticMemoryKDTree(Relation<O> relation) {
    super(relation);
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
    sorted = DBIDUtil.newArray(relation.getDBIDs());
    dims = RelationUtil.dimensionality(relation);
    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    buildTree(0, sorted.size(), 0, comp);
  }

  /**
   * Recursively build the tree by partial sorting. O(n log n) complexity.
   * Apparently there exists a variant in only O(n log log n)? Please
   * contribute!
   * 
   * @param left Interval minimum
   * @param right Interval maximum
   * @param axis Current splitting axis
   * @param comp Comparator
   */
  private void buildTree(int left, int right, int axis, SortDBIDsBySingleDimension comp) {
    final int middle = (left + right) >>> 1;
    comp.setDimension(axis);

    QuickSelect.quickSelect(sorted, comp, left, right, middle);
    final int next = (axis + 1) % dims;
    if(left < middle) {
      buildTree(left, middle, next, comp);
    }
    if(middle + 1 < right) {
      buildTree(middle + 1, right, next, comp);
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

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    DistanceFunction<? super O, D> df = distanceQuery.getDistanceFunction();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistanceFunction) {
      return (KNNQuery<O, D>) new KDTreeKNNQuery((DistanceQuery<O, DoubleDistance>) distanceQuery, (DoubleNorm<? super O>) df);
    }
    if(df instanceof SquaredEuclideanDistanceFunction) {
      return (KNNQuery<O, D>) new KDTreeKNNQuery((DistanceQuery<O, DoubleDistance>) distanceQuery, (DoubleNorm<? super O>) df);
    }
    if(df instanceof SparseLPNormDistanceFunction) {
      return (KNNQuery<O, D>) new KDTreeKNNQuery((DistanceQuery<O, DoubleDistance>) distanceQuery, (DoubleNorm<? super O>) df);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    DistanceFunction<? super O, D> df = distanceQuery.getDistanceFunction();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistanceFunction) {
      return (RangeQuery<O, D>) new KDTreeRangeQuery((DistanceQuery<O, DoubleDistance>) distanceQuery, (DoubleNorm<? super O>) df);
    }
    if(df instanceof SquaredEuclideanDistanceFunction) {
      return (RangeQuery<O, D>) new KDTreeRangeQuery((DistanceQuery<O, DoubleDistance>) distanceQuery, (DoubleNorm<? super O>) df);
    }
    if(df instanceof SparseLPNormDistanceFunction) {
      return (RangeQuery<O, D>) new KDTreeRangeQuery((DistanceQuery<O, DoubleDistance>) distanceQuery, (DoubleNorm<? super O>) df);
    }
    return null;
  }

  /**
   * kNN query for the k-d-tree.
   * 
   * @author Erich Schubert
   */
  public class KDTreeKNNQuery extends AbstractDistanceKNNQuery<O, DoubleDistance> {
    /**
     * Norm to use.
     */
    private DoubleNorm<? super O> norm;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     * @param norm Norm to use
     */
    public KDTreeKNNQuery(DistanceQuery<O, DoubleDistance> distanceQuery, DoubleNorm<? super O> norm) {
      super(distanceQuery);
      this.norm = norm;
    }

    @Override
    public KNNList<DoubleDistance> getKNNForObject(O obj, int k) {
      final DoubleDistanceKNNHeap knns = DBIDUtil.newDoubleDistanceHeap(k);
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
    private double kdKNNSearch(int left, int right, int axis, O query, DoubleDistanceKNNHeap knns, DBIDArrayIter iter, double maxdist) {
      // Look at current node:
      final int middle = (left + right) >>> 1;
      iter.seek(middle);
      O split = relation.get(iter);
      countObjectAccess();

      // Distance to axis:
      final double delta = split.doubleValue(axis) - query.doubleValue(axis);
      final boolean onleft = (delta >= 0);
      final boolean onright = (delta <= 0);

      // Next axis:
      final int next = (axis + 1) % dims;

      // Exact match chance (delta == 0)!
      // process first, then descend both sides.
      if(onleft && onright) {
        double dist = norm.doubleDistance(query, split);
        countDistanceComputation();
        if(dist <= maxdist) {
          iter.seek(middle);
          knns.insert(dist, iter);
          maxdist = knns.doubleKNNDistance();
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
            double dist = norm.doubleDistance(query, split);
            countDistanceComputation();
            if(dist <= maxdist) {
              iter.seek(middle);
              knns.insert(dist, iter);
              maxdist = knns.doubleKNNDistance();
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
            double dist = norm.doubleDistance(query, split);
            countDistanceComputation();
            if(dist <= maxdist) {
              iter.seek(middle);
              knns.insert(dist, iter);
              maxdist = knns.doubleKNNDistance();
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
  public class KDTreeRangeQuery extends AbstractDistanceRangeQuery<O, DoubleDistance> {
    /**
     * Norm to use.
     */
    private DoubleNorm<? super O> norm;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     * @param norm Norm to use
     */
    public KDTreeRangeQuery(DistanceQuery<O, DoubleDistance> distanceQuery, DoubleNorm<? super O> norm) {
      super(distanceQuery);
      this.norm = norm;
    }

    @Override
    public DoubleDistanceDBIDPairList getRangeForObject(O obj, DoubleDistance range) {
      final DoubleDistanceDBIDPairList res = new DoubleDistanceDBIDPairList();
      kdRangeSearch(0, sorted.size(), 0, obj, res, sorted.iter(), range.doubleValue());
      res.sort();
      return res;
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
    private void kdRangeSearch(int left, int right, int axis, O query, ModifiableDoubleDistanceDBIDList res, DBIDArrayIter iter, double radius) {
      // Look at current node:
      final int middle = (left + right) >>> 1;
      iter.seek(middle);
      O split = relation.get(iter);
      countObjectAccess();

      // Distance to axis:
      final double delta = split.doubleValue(axis) - query.doubleValue(axis);
      final boolean onleft = (delta >= 0);
      final boolean onright = (delta <= 0);
      final boolean close = (Math.abs(delta) <= radius);

      // Next axis:
      final int next = (axis + 1) % dims;

      // Current object:
      if(close) {
        double dist = norm.doubleDistance(query, split);
        countDistanceComputation();
        if(dist <= radius) {
          iter.seek(middle);
          res.add(dist, iter);
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
   * @apiviz.stereotype factory
   * @apiviz.has MinimalisticMemoryKDTree
   * 
   * @param <O> Vector type
   */
  @Alias({"minikd", "kd"})
  public static class Factory<O extends NumberVector<?>> implements IndexFactory<O, MinimalisticMemoryKDTree<O>> {
    /**
     * Constructor. Trivial parameterizable.
     */
    public Factory() {
      super();
    }

    @Override
    public MinimalisticMemoryKDTree<O> instantiate(Relation<O> relation) {
      return new MinimalisticMemoryKDTree<>(relation);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }
  }
}
