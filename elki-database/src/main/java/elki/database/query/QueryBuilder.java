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
package elki.database.query;

import java.util.Objects;

import elki.data.NumberVector;
import elki.database.ids.DBIDRef;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.distance.LinearScanEuclideanPrioritySearcher;
import elki.database.query.distance.LinearScanPrioritySearcher;
import elki.database.query.distance.PrimitiveDistanceQuery;
import elki.database.query.knn.*;
import elki.database.query.range.*;
import elki.database.query.rknn.LinearScanRKNNByDBID;
import elki.database.query.rknn.LinearScanRKNNByObject;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.query.similarity.PrimitiveSimilarityQuery;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.DBIDDistance;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.*;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.similarity.DBIDSimilarity;
import elki.similarity.Similarity;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.ELKIServiceRegistry;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.exceptions.AbortException;
import elki.utilities.exceptions.ClassInstantiationException;
import elki.utilities.optionhandling.parameterization.EmptyParameterization;

/**
 * Class to build a query.
 * <p>
 * TODO: move this class to the elki-core-api module,
 * linking the linear-scan dependencies via dynamic class loading instead?
 * <p>
 * TODO: use a service loader to load optimization modules.
 *
 * @author Erich Schubert
 * @since 0.8.0
 * 
 * @param <O> Object type
 */
public class QueryBuilder<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(QueryBuilder.class);

  /**
   * Linear scans only
   */
  public static final int FLAG_LINEAR_ONLY = 0b1;

  /**
   * Optimized queries only, no linear scans
   */
  public static final int FLAG_OPTIMIZED_ONLY = 0b10;

  /**
   * Exact queries only
   */
  public static final int FLAG_EXACT_ONLY = 0b100;

  /**
   * Cheap query only - do not build indexes automatically
   */
  public static final int FLAG_CHEAP_ONLY = 0b1000;

  /**
   * Do not keep auto-generated indexes (c.f., MaterializeKNNPreprocessor).
   */
  public static final int FLAG_NO_CACHE = 0b1_0000;

  /**
   * Flag indicating expected pairwise usage / need for precomputation.
   */
  public static final int FLAG_PRECOMPUTE = 0b10_0000;

  /**
   * Flags that do not allow the optimizer to run.
   */
  public static final int FLAGS_NO_OPTIMIZER = FLAG_LINEAR_ONLY | FLAG_CHEAP_ONLY;

  /**
   * Flags that are not inherited to nested distanceQuery calls.
   */
  public static final int FLAGS_NO_INHERIT = FLAG_PRECOMPUTE;

  /**
   * Global query optimizer, populated at startup.
   */
  private static final QueryOptimizer OPTIMIZER = initStaticOptimizer();

  /**
   * Relation to query.
   */
  private Relation<O> relation;

  /**
   * Distance to query
   */
  private Distance<? super O> distance;

  /**
   * Similarity to query
   */
  private Similarity<? super O> similarity;

  /**
   * Bound distance to query
   */
  private DistanceQuery<O> distQuery;

  /**
   * Bound similarity to query
   */
  private SimilarityQuery<O> simQuery;

  /**
   * Query flags
   */
  private int flags;

  /**
   * Constructor.
   *
   * @param relation Relation to query
   */
  public QueryBuilder(Relation<O> relation, Distance<? super O> distance) {
    this.relation = Objects.requireNonNull(relation);
    this.distance = distance;
  }

  /**
   * Constructor.
   *
   * @param distQuery Query to use
   */
  @SuppressWarnings("unchecked")
  public QueryBuilder(DistanceQuery<? super O> distQuery) {
    this.distQuery = (DistanceQuery<O>) distQuery;
    this.relation = (Relation<O>) distQuery.getRelation();
    this.distance = distQuery.getDistance();
  }

  /**
   * Constructor.
   *
   * @param relation Relation to query
   */
  public QueryBuilder(Relation<O> relation, Similarity<? super O> similarity) {
    this.relation = Objects.requireNonNull(relation);
    this.similarity = similarity;
  }

  /**
   * Constructor.
   *
   * @param simQuery Query to use
   */
  @SuppressWarnings("unchecked")
  public QueryBuilder(SimilarityQuery<? super O> simQuery) {
    this.simQuery = (SimilarityQuery<O>) simQuery;
    this.relation = (Relation<O>) simQuery.getRelation();
    this.similarity = simQuery.getSimilarity();
  }

  /**
   * Only build linear-scan queries, useful as reference for evaluation of
   * approximate methods. This can obviously not be combined with
   * {@link #FLAG_OPTIMIZED_ONLY}.
   * 
   * @return query builder, for chaining
   */
  public QueryBuilder<O> linearOnly() {
    assert (flags & FLAG_OPTIMIZED_ONLY) == 0;
    this.flags |= FLAG_LINEAR_ONLY;
    return this;
  }

  /**
   * Only query for optimized functions, returning null otherwise. This allows
   * the user to choose fallback strategies instead. This can obviously not be
   * combined with {@link #FLAG_LINEAR_ONLY}.
   * 
   * @return query builder, for chaining
   */
  public QueryBuilder<O> optimizedOnly() {
    assert (flags & FLAG_LINEAR_ONLY) == 0;
    this.flags |= FLAG_OPTIMIZED_ONLY;
    return this;
  }

  /**
   * Only accept exact methods, no approximate methods.
   * 
   * @return query builder, for chaining
   */
  public QueryBuilder<O> exactOnly() {
    this.flags |= FLAG_EXACT_ONLY;
    return this;
  }

  /**
   * Only perform the cheapest optimizations, used to indicate that the query
   * will not be used much.
   * 
   * @return query builder, for chaining
   */
  public QueryBuilder<O> cheapOnly() {
    this.flags |= FLAG_CHEAP_ONLY;
    return this;
  }

  /**
   * Avoid caching optimizations such as computing a distance matrix, because
   * the results will be used to, e.g., build a similar data structure.
   * 
   * @return query builder, for chaining
   */
  public QueryBuilder<O> noCache() {
    this.flags |= FLAG_NO_CACHE;
    return this;
  }

  /**
   * Indicate that the almost all pairwise distances / each objects knn will be
   * used multiple times, and results should be precomputed and cached.
   * 
   * @return query builder, for chaining
   */
  public QueryBuilder<O> precomputed() {
    this.flags |= FLAG_PRECOMPUTE;
    return this;
  }

  /**
   * Build a distance query.
   *
   * @return distance query
   */
  public DistanceQuery<O> distanceQuery() {
    if(distQuery != null) {
      return distQuery;
    }
    if(distance == null) {
      throw new IllegalStateException("Distance query requested for 'null' distance!");
    }
    for(It<DistanceIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(DistanceIndex.class); it.valid(); it.advance()) {
      distQuery = it.get().getDistanceQuery(distance);
      logUsing(it.get(), "distance", distQuery != null);
      if(distQuery != null) {
        return distQuery;
      }
    }
    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      distQuery = OPTIMIZER.getDistanceQuery(relation, distance, flags);
      if(distQuery != null) {
        return distQuery;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0 && !(distance instanceof DBIDDistance)) {
      return null; // Disallowed
    }
    if((flags & FLAG_PRECOMPUTE) != 0) {
      LOG.warning("The algorithm requested a distance matrix, but we could not precompute one.\n This may or may not be very expensive / slow.", new Throwable());
    }
    return (distQuery = distance.instantiate(relation));
  }

  /**
   * Build a similarity query.
   *
   * @return similarity query
   */
  public SimilarityQuery<O> similarityQuery() {
    if(simQuery != null) {
      return simQuery;
    }
    if(similarity == null) {
      throw new IllegalStateException("Similarity query requested for 'null' similarity!");
    }
    for(It<SimilarityIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(SimilarityIndex.class); it.valid(); it.advance()) {
      simQuery = it.get().getSimilarityQuery(similarity);
      logUsing(it.get(), "similarity", simQuery != null);
      if(simQuery != null) {
        return simQuery;
      }
    }
    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      simQuery = OPTIMIZER.getSimilarityQuery(relation, similarity, flags);
      if(simQuery != null) {
        return simQuery;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0 && !(similarity instanceof DBIDSimilarity)) {
      return null; // Disallowed
    }
    return similarity.instantiate(relation);
  }

  /**
   * Build a k-nearest-neighbors query; if possible also give a maximum k.
   *
   * @return knn query
   */
  public KNNSearcher<O> kNNByObject() {
    return kNNByObject(Integer.MAX_VALUE);
  }

  /**
   * Build a k-nearest-neighbors query.
   * 
   * @param maxk Maximum k that will be used later.
   * @return knn query
   */
  @SuppressWarnings("unchecked")
  public KNNSearcher<O> kNNByObject(int maxk) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<KNNIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(KNNIndex.class); it.valid(); it.advance()) {
      KNNSearcher<O> q = it.get().kNNByObject(distanceQuery, maxk, flags);
      logUsing(it.get(), "kNN", q != null);
      if(q != null) {
        return q;
      }
    }
    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      KNNSearcher<O> q = OPTIMIZER.kNNByObject(relation, distanceQuery, maxk, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("knn");
    // Slight optimizations of linear scans for primitive functions
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistance.STATIC.equals(pdq.getDistance())) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return (KNNSearcher<O>) new LinearScanEuclideanKNNByObject<>(ndq);
      }
      return new LinearScanPrimitiveKNNByObject<>(pdq);
    }
    return new LinearScanKNNByObject<>(distanceQuery);
  }

  /**
   * Build a k-nearest-neighbors query; if possible also give a maximum k.
   *
   * @return knn query
   */
  public KNNSearcher<DBIDRef> kNNByDBID() {
    return kNNByDBID(Integer.MAX_VALUE);
  }

  /**
   * Build a k-nearest-neighbors query.
   * 
   * @param maxk Maximum k that will be used later.
   * @return knn query
   */
  @SuppressWarnings("unchecked")
  public KNNSearcher<DBIDRef> kNNByDBID(int maxk) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<KNNIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(KNNIndex.class); it.valid(); it.advance()) {
      KNNSearcher<DBIDRef> q = it.get().kNNByDBID(distanceQuery, maxk, flags);
      logUsing(it.get(), "kNN", q != null);
      if(q != null) {
        return q;
      }
    }
    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      KNNSearcher<DBIDRef> q = OPTIMIZER.kNNByDBID(relation, distanceQuery, maxk, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("knn");
    // Slight optimizations of linear scans for primitive functions
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistance.STATIC.equals(pdq.getDistance())) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return WrappedKNNDBIDByLookup.wrap(ndq.getRelation(), new LinearScanEuclideanKNNByObject<>(ndq));
      }
      return WrappedKNNDBIDByLookup.wrap(pdq.getRelation(), new LinearScanPrimitiveKNNByObject<>(pdq));
    }
    return new LinearScanKNNByDBID<>(distanceQuery);
  }

  /**
   * Build a range query; if possible also give a maximum query radius.
   *
   * @return range query
   */
  public RangeSearcher<O> rangeByObject() {
    return rangeByObject(Double.POSITIVE_INFINITY);
  }

  /**
   * Build a range query with maximum radius.
   *
   * @param maxrange Maximum radius that will be used.
   * @return range query
   */
  @SuppressWarnings("unchecked")
  public RangeSearcher<O> rangeByObject(double maxrange) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<RangeIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(RangeIndex.class); it.valid(); it.advance()) {
      RangeSearcher<O> q = it.get().rangeByObject(distanceQuery, maxrange, flags);
      logUsing(it.get(), "range", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      RangeSearcher<O> q = OPTIMIZER.rangeByObject(relation, distanceQuery, maxrange, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("range");
    // Slight optimizations of linear scans
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistance.STATIC.equals(distance)) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return (RangeSearcher<O>) new LinearScanEuclideanRangeByObject<>(ndq);
      }
      return new LinearScanPrimitiveDistanceRangeByObject<>(pdq);
    }
    return new LinearScanDistanceRangeByObject<>(distanceQuery);
  }

  /**
   * Build a range query; if possible also give a maximum query radius.
   *
   * @return range query
   */
  public RangeSearcher<DBIDRef> rangeByDBID() {
    return rangeByDBID(Double.POSITIVE_INFINITY);
  }

  /**
   * Build a range query with maximum radius.
   *
   * @param maxrange Maximum radius that will be used.
   * @return range query
   */
  @SuppressWarnings("unchecked")
  public RangeSearcher<DBIDRef> rangeByDBID(double maxrange) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<RangeIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(RangeIndex.class); it.valid(); it.advance()) {
      RangeSearcher<DBIDRef> q = it.get().rangeByDBID(distanceQuery, maxrange, flags);
      logUsing(it.get(), "range", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      RangeSearcher<DBIDRef> q = OPTIMIZER.rangeByDBID(relation, distanceQuery, maxrange, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("range");
    // Slight optimizations of linear scans
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistance.STATIC.equals(distance)) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return WrappedRangeDBIDByLookup.wrap(ndq.getRelation(), new LinearScanEuclideanRangeByObject<>(ndq));
      }
      return WrappedRangeDBIDByLookup.wrap(pdq.getRelation(), new LinearScanPrimitiveDistanceRangeByObject<>(pdq));
    }
    return new LinearScanDistanceRangeByDBID<>(distanceQuery);
  }

  /**
   * Build a similarity range query; if possible also specify the least
   * selective
   * threshold.
   *
   * @return Similarity range query
   */
  public RangeSearcher<O> similarityRangeByObject() {
    return this.similarityRangeByObject(Double.NEGATIVE_INFINITY);
  }

  /**
   * Build a similarity range query.
   *
   * @param threshold smallest similarity that will be queried later
   * @return Similarity range query
   */
  public RangeSearcher<O> similarityRangeByObject(double threshold) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    SimilarityQuery<O> simQuery = similarityQuery();
    flags ^= precompute; // Restore
    for(It<SimilarityRangeIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(SimilarityRangeIndex.class); it.valid(); it.advance()) {
      RangeSearcher<O> q = it.get().similarityRangeByObject(simQuery, threshold, flags);
      logUsing(it.get(), "similarity", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      RangeSearcher<O> q = OPTIMIZER.similarityRangeByObject(relation, simQuery, threshold, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("simrange");
    // Slight optimizations of linear scans
    return simQuery instanceof PrimitiveSimilarityQuery ? //
        new LinearScanPrimitiveSimilarityRangeByObject<>((PrimitiveSimilarityQuery<O>) simQuery) : //
        new LinearScanSimilarityRangeByObject<>(simQuery);
  }

  /**
   * Build a similarity range query; if possible also specify the least
   * selective
   * threshold.
   *
   * @return Similarity range query
   */
  public RangeSearcher<DBIDRef> similarityRangeByDBID() {
    return this.similarityRangeByDBID(Double.NEGATIVE_INFINITY);
  }

  /**
   * Build a similarity range query.
   *
   * @param threshold smallest similarity that will be queried later
   * @return Similarity range query
   */
  public RangeSearcher<DBIDRef> similarityRangeByDBID(double threshold) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    SimilarityQuery<O> simQuery = similarityQuery();
    flags ^= precompute; // Restore
    for(It<SimilarityRangeIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(SimilarityRangeIndex.class); it.valid(); it.advance()) {
      RangeSearcher<DBIDRef> q = it.get().similarityRangeByDBID(simQuery, threshold, flags);
      logUsing(it.get(), "similarity", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      RangeSearcher<DBIDRef> q = OPTIMIZER.similarityRangeByDBID(relation, simQuery, threshold, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("simrange");
    // Slight optimizations of linear scans
    return simQuery instanceof PrimitiveSimilarityQuery ? //
        WrappedRangeDBIDByLookup.wrap(simQuery.getRelation(), new LinearScanPrimitiveSimilarityRangeByObject<>((PrimitiveSimilarityQuery<O>) simQuery)) : //
        new LinearScanSimilarityRangeByDBID<>(simQuery);
  }

  /**
   * Build a reverse k-nearest neighbors query.
   *
   * @return rkNN query
   */
  public RKNNSearcher<O> rKNNByObject() {
    return rKNNByObject(Integer.MAX_VALUE);
  }

  /**
   * Build a reverse k-nearest neighbors query.
   *
   * @param k k to be used; many indexes cannot support arbitrary k.
   * @return rkNN query
   */
  public RKNNSearcher<O> rKNNByObject(int k) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<RKNNIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(RKNNIndex.class); it.valid(); it.advance()) {
      RKNNSearcher<O> q = it.get().rkNNByObject(distanceQuery, k, flags);
      logUsing(it.get(), "RkNN", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      RKNNSearcher<O> q = OPTIMIZER.rkNNByObject(relation, distanceQuery, k, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("rknn");
    return new LinearScanRKNNByObject<>(distanceQuery, kNNByDBID()); // Default
  }

  /**
   * Build a reverse k-nearest neighbors query.
   *
   * @return rkNN query
   */
  public RKNNSearcher<DBIDRef> rKNNByDBID() {
    return rKNNByDBID(Integer.MAX_VALUE);
  }

  /**
   * Build a reverse k-nearest neighbors query.
   *
   * @param k k to be used; many indexes cannot support arbitrary k.
   * @return rkNN query
   */
  public RKNNSearcher<DBIDRef> rKNNByDBID(int k) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<RKNNIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(RKNNIndex.class); it.valid(); it.advance()) {
      RKNNSearcher<DBIDRef> q = it.get().rkNNByDBID(distanceQuery, k, flags);
      logUsing(it.get(), "RkNN", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      RKNNSearcher<DBIDRef> q = OPTIMIZER.rkNNByDBID(relation, distanceQuery, k, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("rknn");
    return new LinearScanRKNNByDBID<>(distanceQuery, kNNByDBID()); // Default
  }

  /**
   * Build a priority searcher.
   *
   * @return priority searcher
   */
  public PrioritySearcher<O> priorityByObject() {
    return priorityByObject(Double.POSITIVE_INFINITY);
  }

  /**
   * Build a priority searcher.
   *
   * @param maxrange maximum cut-off
   * @return priority searcher
   */
  @SuppressWarnings("unchecked")
  public PrioritySearcher<O> priorityByObject(double maxrange) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<DistancePriorityIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(DistancePriorityIndex.class); it.valid(); it.advance()) {
      PrioritySearcher<O> q = it.get().priorityByObject(distanceQuery, maxrange, flags);
      logUsing(it.get(), "priority", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      PrioritySearcher<O> q = OPTIMIZER.priorityByObject(relation, distanceQuery, maxrange, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("priority");
    if(distanceQuery instanceof PrimitiveDistanceQuery && EuclideanDistance.STATIC.equals(distance)) {
      PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) distanceQuery;
      return (PrioritySearcher<O>) new LinearScanEuclideanPrioritySearcher.ByObject<>(ndq);
    }
    return new LinearScanPrioritySearcher.ByObject<>(distanceQuery);
  }

  /**
   * Build a priority searcher.
   *
   * @return priority searcher
   */
  public PrioritySearcher<DBIDRef> priorityByDBID() {
    return priorityByDBID(Double.POSITIVE_INFINITY);
  }

  /**
   * Build a priority searcher.
   *
   * @param maxrange maximum cut-off
   * @return priority searcher
   */
  @SuppressWarnings("unchecked")
  public PrioritySearcher<DBIDRef> priorityByDBID(double maxrange) {
    int precompute = flags & FLAG_PRECOMPUTE;
    flags ^= precompute; // Mask
    DistanceQuery<O> distanceQuery = distanceQuery();
    flags ^= precompute; // Restore
    for(It<DistancePriorityIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(DistancePriorityIndex.class); it.valid(); it.advance()) {
      PrioritySearcher<DBIDRef> q = it.get().priorityByDBID(distanceQuery, maxrange, flags);
      logUsing(it.get(), "priority", q != null);
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    if((flags & FLAGS_NO_OPTIMIZER) == 0) {
      PrioritySearcher<DBIDRef> q = OPTIMIZER.priorityByDBID(relation, distanceQuery, maxrange, flags);
      if(q != null) {
        return q;
      }
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("priority");
    if(distanceQuery instanceof PrimitiveDistanceQuery && EuclideanDistance.STATIC.equals(distance)) {
      PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) distanceQuery;
      return (PrioritySearcher<DBIDRef>) new LinearScanEuclideanPrioritySearcher.ByDBID<>(ndq);
    }
    return new LinearScanPrioritySearcher.ByDBID<>(distanceQuery);
  }

  /**
   * Log if we use a particular index.
   *
   * @param index Index tested
   * @param kind Query kind
   * @param used true if the index is used.
   */
  private void logUsing(Index index, String kind, boolean used) {
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest((used ? "Using" : "Not using") + " index for " + kind + " query: " + index);
    }
  }

  /**
   * Log if we have to fall back to a linear scan.
   * 
   * @param kind Query kind
   */
  private void logNotAccelerated(String kind) {
    if(LOG.isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder(200) //
          .append("Fallback to linear scan for ").append(kind) //
          .append(" query - no index was able to accelerate this query:");
      if(distance != null) {
        buf.append("\nDistance: ").append(distance.toString());
      }
      else if(distQuery != null) {
        buf.append("\nDistance: ").append(distQuery.getDistance().toString());
      }
      if(similarity != null) {
        buf.append("\nSimilarity: ").append(similarity.toString());
      }
      else if(simQuery != null) {
        buf.append("\nSimilarity: ").append(simQuery.getSimilarity().toString());
      }
      if(flags != 0) {
        buf.append(", hints:") //
            .append(((flags & FLAG_LINEAR_ONLY) == 0) ? "" : " linear") //
            .append(((flags & FLAG_OPTIMIZED_ONLY) == 0) ? "" : " optimized") //
            .append(((flags & FLAG_EXACT_ONLY) == 0) ? "" : " exact") //
            .append(((flags & FLAG_CHEAP_ONLY) == 0) ? "" : " cheap") //
            .append(((flags & FLAG_NO_CACHE) == 0) ? "" : " no-cache");
      }
      LOG.debugFinest(buf.toString());
    }
  }

  /**
   * Initialization method, which sets {@link #OPTIMIZER}.
   *
   * @return Optimizer
   */
  private static QueryOptimizer initStaticOptimizer() {
    if(OPTIMIZER != null) {
      return OPTIMIZER;
    }
    String opt = System.getenv("elki.optimizer");
    if(opt != null) {
      if(opt.isEmpty()) { // Disable
        LOG.warning("Optimizer disabled.");
        return DisableQueryOptimizer.STATIC;
      }
      try {
        Class<? extends QueryOptimizer> clz = ELKIServiceRegistry.findImplementation(QueryOptimizer.class, opt);
        if(clz == null) {
          throw new AbortException("Could not find query optimizer: " + opt);
        }
        return ClassGenericsUtil.tryInstantiate(QueryOptimizer.class, clz, new EmptyParameterization());
      }
      catch(ClassInstantiationException e) {
        throw new AbortException("Failed to initialize query optimizer: " + opt, e);
      }
    }
    // Default optimizer
    return new EmpiricalQueryOptimizer();
  }
}
