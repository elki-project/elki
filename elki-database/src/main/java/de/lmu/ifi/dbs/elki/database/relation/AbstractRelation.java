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
package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DBIDDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.DBIDSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.DistanceIndex;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.SimilarityIndex;
import de.lmu.ifi.dbs.elki.index.SimilarityRangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for relations.
 *
 * @author Erich Schubert
 * @since 0.1
 *
 * @param <O> Data type
 */
public abstract class AbstractRelation<O> extends AbstractHierarchicalResult implements Relation<O> {
  /**
   * Constructor.
   */
  public AbstractRelation() {
    super();
  }

  @Override
  public DistanceQuery<O> getDistanceQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("Distance query requested for 'null' distance!");
    }
    for(It<DistanceIndex<O>> it = getHierarchy().iterChildrenReverse(this).filter(DistanceIndex.class); it.valid(); it.advance()) {
      DistanceQuery<O> q = it.get().getDistanceQuery(distanceFunction, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for distance query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }
    for(Object o : hints) {
      if(o == DatabaseQuery.HINT_OPTIMIZED_ONLY && !(distanceFunction instanceof DBIDDistanceFunction)) {
        return null; // Linear scan is not desirable.
      }
    }
    return distanceFunction.instantiate(this);
  }

  @Override
  public SimilarityQuery<O> getSimilarityQuery(SimilarityFunction<? super O> similarityFunction, Object... hints) {
    if(similarityFunction == null) {
      throw new AbortException("Similarity query requested for 'null' similarity!");
    }
    for(It<SimilarityIndex<O>> it = getHierarchy().iterChildrenReverse(this).filter(SimilarityIndex.class); it.valid(); it.advance()) {
      SimilarityQuery<O> q = it.get().getSimilarityQuery(similarityFunction, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for similarity query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }
    for(Object o : hints) {
      if(o == DatabaseQuery.HINT_OPTIMIZED_ONLY && !(similarityFunction instanceof DBIDSimilarityFunction)) {
        return null; // Linear scan is not desirable.
      }
    }
    return similarityFunction.instantiate(this);
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    for(It<KNNIndex<O>> it = getHierarchy().iterChildrenReverse(this).filter(KNNIndex.class); it.valid(); it.advance()) {
      KNNQuery<O> q = it.get().getKNNQuery(distanceQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for kNN query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    if(getLogger().isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder();
      buf.append("Fallback to linear scan - no index was able to accelerate this query.\n");
      buf.append("Distance query: ").append(distanceQuery).append('\n');
      if(hints.length > 0) {
        buf.append("Hints:");
        for(Object o : hints) {
          buf.append(' ').append(o);
        }
      }
      getLogger().debugFinest(buf.toString());
    }
    return QueryUtil.getLinearScanKNNQuery(distanceQuery);
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(It<RangeIndex<O>> it = getHierarchy().iterChildrenReverse(this).filter(RangeIndex.class); it.valid(); it.advance()) {
      RangeQuery<O> q = it.get().getRangeQuery(distanceQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for range query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    if(getLogger().isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder();
      buf.append("Fallback to linear scan - no index was able to accelerate this query.\n");
      buf.append("Distance query: ").append(distanceQuery).append('\n');
      if(hints.length > 0) {
        buf.append("Hints:");
        for(Object o : hints) {
          buf.append(' ').append(o);
        }
      }
      getLogger().debugFinest(buf.toString());
    }
    return QueryUtil.getLinearScanRangeQuery(distanceQuery);
  }

  @Override
  public RangeQuery<O> getSimilarityRangeQuery(SimilarityQuery<O> simQuery, Object... hints) {
    if(simQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(It<SimilarityRangeIndex<O>> it = getHierarchy().iterChildrenReverse(this).filter(SimilarityRangeIndex.class); it.valid(); it.advance()) {
      RangeQuery<O> q = it.get().getSimilarityRangeQuery(simQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for range query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    if(getLogger().isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder();
      buf.append("Fallback to linear scan - no index was able to accelerate this query.\n");
      buf.append("Distance query: ").append(simQuery).append('\n');
      if(hints.length > 0) {
        buf.append("Hints:");
        for(Object o : hints) {
          buf.append(' ').append(o);
        }
      }
      getLogger().debugFinest(buf.toString());
    }
    return QueryUtil.getLinearScanSimilarityRangeQuery(simQuery);
  }

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    for(It<RKNNIndex<O>> it = getHierarchy().iterChildrenReverse(this).filter(RKNNIndex.class); it.valid(); it.advance()) {
      RKNNQuery<O> q = it.get().getRKNNQuery(distanceQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for RkNN query: " + it.get());
      }
      if(q != null) {
        return q;
      }
    }

    Integer maxk = null;
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
      if(hint instanceof Integer) {
        maxk = (Integer) hint;
      }
    }
    if(getLogger().isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder();
      buf.append("Fallback to linear scan - no index was able to accelerate this query.\n");
      buf.append("Distance query: ").append(distanceQuery).append('\n');
      if(hints.length > 0) {
        buf.append("Hints:");
        for(Object o : hints) {
          buf.append(' ').append(o);
        }
      }
      getLogger().debugFinest(buf.toString());
    }
    KNNQuery<O> knnQuery = getKNNQuery(distanceQuery, DatabaseQuery.HINT_BULK, maxk);
    return new LinearScanRKNNQuery<>(distanceQuery, knnQuery, maxk);
  }

  /**
   * Get the class logger.
   *
   * @return Logger
   */
  abstract protected Logging getLogger();
}
