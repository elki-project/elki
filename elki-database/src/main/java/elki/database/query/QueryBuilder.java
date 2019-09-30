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
package elki.database.query;

import java.util.Objects;

import elki.data.NumberVector;
import elki.database.query.distance.*;
import elki.database.query.knn.KNNQuery;
import elki.database.query.knn.LinearScanDistanceKNNQuery;
import elki.database.query.knn.LinearScanEuclideanDistanceKNNQuery;
import elki.database.query.knn.LinearScanPrimitiveDistanceKNNQuery;
import elki.database.query.range.*;
import elki.database.query.rknn.LinearScanRKNNQuery;
import elki.database.query.rknn.RKNNQuery;
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
 *
 * @author Erich Schubert
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
  public static int FLAG_LINEAR_ONLY = 0b1;

  /**
   * Optimized queries only, no linear scans
   */
  public static int FLAG_OPTIMIZED_ONLY = 0b10;

  /**
   * Exact queries only
   */
  public static int FLAG_EXACT_ONLY = 0b100;

  /**
   * Cheap query only - do not build indexes automatically
   */
  public static int FLAG_CHEAP_ONLY = 0b1000;

  /**
   * Do not keep auto-generated indexes (c.f., MaterializeKNNPreprocessor).
   */
  public static final int FLAG_NO_CACHE = 0b1_0000;

  /**
   * Flags that do not allow the optimizer to run.
   */
  public static int FLAGS_NO_OPTIMIZER = FLAG_LINEAR_ONLY | FLAG_CHEAP_ONLY;

  /**
   * Global query optimizer, populated at startup.
   */
  private static final QueryOptimizer OPTIMIZER = initStaticOptimizer();

  /**
   * Initialization method, which sets {@link #OPTIMIZER}.
   *
   * @return Optimizer
   */
  static QueryOptimizer initStaticOptimizer() {
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

  public QueryBuilder<O> linearOnly() {
    assert (flags & FLAG_OPTIMIZED_ONLY) == 0;
    this.flags |= FLAG_LINEAR_ONLY;
    return this;
  }

  public QueryBuilder<O> optimizedOnly() {
    assert (flags & FLAG_LINEAR_ONLY) == 0;
    this.flags |= FLAG_OPTIMIZED_ONLY;
    return this;
  }

  public QueryBuilder<O> exactOnly() {
    this.flags |= FLAG_EXACT_ONLY;
    return this;
  }

  public QueryBuilder<O> cheapOnly() {
    this.flags |= FLAG_CHEAP_ONLY;
    return this;
  }

  public QueryBuilder<O> noCache() {
    this.flags |= FLAG_NO_CACHE;
    return this;
  }

  public DistanceQuery<O> distanceQuery() {
    if(distQuery != null) {
      return distQuery;
    }
    if(distance == null) {
      throw new AbortException("Distance query requested for 'null' distance!");
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
    return (distQuery = distance.instantiate(relation));
  }

  public SimilarityQuery<O> similarityQuery() {
    if(simQuery != null) {
      return simQuery;
    }
    if(similarity == null) {
      throw new AbortException("Similarity query requested for 'null' similarity!");
    }
    for(It<SimilarityIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(SimilarityIndex.class); it.valid(); it.advance()) {
      simQuery = it.get().getSimilarityQuery(similarity);
      logUsing(it.get(), "similarity", simQuery != null);
      if(simQuery != null) {
        return simQuery;
      }
    }
    // Use optimizer
    simQuery = OPTIMIZER.getSimilarityQuery(relation, similarity, flags);
    if(simQuery != null) {
      return simQuery;
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0 && !(similarity instanceof DBIDSimilarity)) {
      return null; // Disallowed
    }
    return similarity.instantiate(relation);
  }

  public KNNQuery<O> kNNQuery() {
    return kNNQuery(Integer.MAX_VALUE);
  }

  @SuppressWarnings("unchecked")
  public KNNQuery<O> kNNQuery(int maxk) {
    DistanceQuery<O> distanceQuery = distanceQuery();
    for(It<KNNIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(KNNIndex.class); it.valid(); it.advance()) {
      KNNQuery<O> q = it.get().getKNNQuery(distanceQuery, maxk, flags);
      logUsing(it.get(), "kNN", q != null);
      if(q != null) {
        return q;
      }
    }
    // Use optimizer
    KNNQuery<O> q = OPTIMIZER.getKNNQuery(relation, distanceQuery, maxk, flags);
    if(q != null) {
      return q;
    }
    if(q != null || (flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("knn");
    // Slight optimizations of linear scans for primitive functions
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistance.STATIC.equals(pdq.getDistance())) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return (KNNQuery<O>) new LinearScanEuclideanDistanceKNNQuery<>(ndq);
      }
      return new LinearScanPrimitiveDistanceKNNQuery<>(pdq);
    }
    return new LinearScanDistanceKNNQuery<>(distanceQuery);
  }

  public RangeQuery<O> rangeQuery() {
    return rangeQuery(Double.POSITIVE_INFINITY);
  }

  @SuppressWarnings("unchecked")
  public RangeQuery<O> rangeQuery(double maxrange) {
    DistanceQuery<O> distanceQuery = distanceQuery();
    for(It<RangeIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(RangeIndex.class); it.valid(); it.advance()) {
      RangeQuery<O> q = it.get().getRangeQuery(distanceQuery, maxrange, flags);
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest((q != null ? "Using" : "Not using") + " index for range query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    RangeQuery<O> q = OPTIMIZER.getRangeQuery(relation, distanceQuery, maxrange, flags);
    if(q != null) {
      return q;
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
        return (RangeQuery<O>) new LinearScanEuclideanDistanceRangeQuery<>(ndq);
      }
      return new LinearScanPrimitiveDistanceRangeQuery<>(pdq);
    }
    return new LinearScanDistanceRangeQuery<>(distanceQuery);
  }

  public RangeQuery<O> similarityRangeQuery() {
    return this.similarityRangeQuery(Double.NEGATIVE_INFINITY);
  }

  public RangeQuery<O> similarityRangeQuery(double maxrange) {
    SimilarityQuery<O> simQuery = similarityQuery();
    for(It<SimilarityRangeIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(SimilarityRangeIndex.class); it.valid(); it.advance()) {
      RangeQuery<O> q = it.get().getSimilarityRangeQuery(simQuery, maxrange, flags);
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest((q != null ? "Using" : "Not using") + " index for range query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    RangeQuery<O> q = OPTIMIZER.getSimilarityRangeQuery(relation, simQuery, maxrange, flags);
    if(q != null) {
      return q;
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("simrange");
    // Slight optimizations of linear scans
    return simQuery instanceof PrimitiveSimilarityQuery ? //
        new LinearScanPrimitiveSimilarityRangeQuery<>((PrimitiveSimilarityQuery<O>) simQuery) : //
        new LinearScanSimilarityRangeQuery<>(simQuery);
  }

  public RKNNQuery<O> rKNNQuery() {
    return rKNNQuery(Integer.MAX_VALUE);
  }

  public RKNNQuery<O> rKNNQuery(int maxk) {
    DistanceQuery<O> distanceQuery = distanceQuery();
    for(It<RKNNIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(RKNNIndex.class); it.valid(); it.advance()) {
      RKNNQuery<O> q = it.get().getRKNNQuery(distanceQuery, maxk, flags);
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest((q != null ? "Using" : "Not using") + " index for RkNN query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    RKNNQuery<O> q = OPTIMIZER.getRKNNQuery(relation, distanceQuery, maxk, flags);
    if(q != null) {
      return q;
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("rknn");
    return new LinearScanRKNNQuery<>(distanceQuery, kNNQuery(), maxk); // Default
  }

  public DistancePrioritySearcher<O> prioritySearcher() {
    return prioritySearcher(Double.POSITIVE_INFINITY);
  }

  @SuppressWarnings("unchecked")
  public DistancePrioritySearcher<O> prioritySearcher(double maxrange) {
    DistanceQuery<O> distanceQuery = distanceQuery();
    for(It<DistancePriorityIndex<O>> it = Metadata.hierarchyOf(relation).iterChildrenReverse().filter(DistancePriorityIndex.class); it.valid(); it.advance()) {
      DistancePrioritySearcher<O> q = it.get().getPriorityQuery(distanceQuery, maxrange, flags);
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest((q != null ? "Using" : "Not using") + " index for range query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Use optimizer
    DistancePrioritySearcher<O> q = OPTIMIZER.getPrioritySearcher(relation, distanceQuery, maxrange, flags);
    if(q != null) {
      return q;
    }
    if((flags & FLAG_OPTIMIZED_ONLY) != 0) {
      return null;
    }
    logNotAccelerated("priority");
    if(EuclideanDistance.STATIC.equals(distance)) {
      final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) distanceQuery;
      return (DistancePrioritySearcher<O>) new LinearScanEuclideanDistancePrioritySearcher<>(ndq);
    }
    return new LinearScanDistancePrioritySearcher<>(distanceQuery);
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
}
