package de.lmu.ifi.dbs.elki.database.relation;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.DistanceIndex;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for relations.
 *
 * @author Erich Schubert
 * @since 0.7.0
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
    for(Iter<Result> it = getHierarchy().iterChildrenReverse(this); it.valid(); it.advance()) {
      if(!(it.get() instanceof Index)) {
        continue;
      }
      Index idx = (Index) it.get();
      if(idx instanceof DistanceIndex) {
        @SuppressWarnings("unchecked")
        final DistanceIndex<O> distanceIndex = (DistanceIndex<O>) idx;
        DistanceQuery<O> q = distanceIndex.getDistanceQuery(distanceFunction, hints);
        if(getLogger().isDebuggingFinest()) {
          getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for distance query: " + idx);
        }
        if(q != null) {
          return q;
        }
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
    // TODO: add indexing support for similarities!
    return similarityFunction.instantiate(this);
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    for(Iter<Result> it = getHierarchy().iterChildrenReverse(this); it.valid(); it.advance()) {
      if(!(it.get() instanceof KNNIndex)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      final KNNIndex<O> knnIndex = (KNNIndex<O>) it.get();
      KNNQuery<O> q = knnIndex.getKNNQuery(distanceQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for kNN query: " + knnIndex);
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
  public KNNQuery<O> getKNNQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    DistanceQuery<O> distanceQuery = getDistanceQuery(distanceFunction, hints);
    return getKNNQuery(distanceQuery, hints);
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(Iter<Result> it = getHierarchy().iterChildrenReverse(this); it.valid(); it.advance()) {
      if(!(it.get() instanceof RangeIndex)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      final RangeIndex<O> rangeIndex = (RangeIndex<O>) it.get();
      RangeQuery<O> q = rangeIndex.getRangeQuery(distanceQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for range query: " + rangeIndex);
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
  public RangeQuery<O> getRangeQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    DistanceQuery<O> distanceQuery = getDistanceQuery(distanceFunction, hints);
    return getRangeQuery(distanceQuery, hints);
  }

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    for(Iter<Result> it = getHierarchy().iterChildrenReverse(this); it.valid(); it.advance()) {
      if(!(it.get() instanceof RKNNIndex)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      final RKNNIndex<O> rknnIndex = (RKNNIndex<O>) it.get();
      RKNNQuery<O> q = rknnIndex.getRKNNQuery(distanceQuery, hints);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest((q != null ? "Using" : "Not using") + " index for RkNN query: " + rknnIndex);
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

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    DistanceQuery<O> distanceQuery = getDistanceQuery(distanceFunction, hints);
    return getRKNNQuery(distanceQuery, hints);
  }

  /**
   * Get the class logger.
   *
   * @return Logger
   */
  abstract protected Logging getLogger();
}
