package de.lmu.ifi.dbs.elki.database;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract base class for database API implementations. Provides default
 * management of relations, indexes and events as well as default query
 * matching.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DatabaseEventManager
 * @apiviz.has IndexFactory
 */
public abstract class AbstractDatabase extends AbstractHierarchicalResult implements Database {
  /**
   * Parameter to specify the indexes to use.
   * <p>
   * Key: {@code -db.index}
   * </p>
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("db.index", "Database indexes to add.");

  /**
   * The event manager, collects events and fires them on demand.
   */
  protected final DatabaseEventManager eventManager = new DatabaseEventManager();

  /**
   * The relations we manage.
   */
  protected final List<Relation<?>> relations = new java.util.Vector<Relation<?>>();

  /**
   * Indexes
   */
  protected final List<Index> indexes = new java.util.Vector<Index>();

  /**
   * Index factories
   */
  protected final Collection<IndexFactory<?, ?>> indexFactories = new java.util.Vector<IndexFactory<?, ?>>();

  /**
   * Constructor.
   */
  public AbstractDatabase() {
    super();
  }

  @Override
  public void addIndex(Index index) {
    this.indexes.add(index);
    // TODO: actually add index to the representation used?
    this.addChildResult(index);
  }

  @Override
  public List<Index> getIndexes() {
    return Collections.unmodifiableList(this.indexes);
  }

  @Override
  public void removeIndex(Index index) {
    this.indexes.remove(index);
    this.getHierarchy().remove(this, index);
  }

  @Override
  public SingleObjectBundle getBundle(DBIDRef id) {
    assert (id != null);
    // TODO: ensure that the ID actually exists in the database?
    try {
      // Build an object package
      SingleObjectBundle ret = new SingleObjectBundle();
      for(Relation<?> relation : relations) {
        ret.append(relation.getDataTypeInformation(), relation.get(id));
      }
      return ret;
    }
    catch(RuntimeException e) {
      if(id == null) {
        throw new UnsupportedOperationException("AbstractDatabase.getPackage(null) called!");
      }
      // throw e upwards.
      throw e;
    }
  }

  @Override
  public Collection<Relation<?>> getRelations() {
    return Collections.unmodifiableCollection(relations);
  }

  @SuppressWarnings({ "unchecked" })
  @Override
  public <O> Relation<O> getRelation(TypeInformation restriction, Object... hints) throws NoSupportedDataTypeException {
    // Get first match
    for(Relation<?> relation : relations) {
      if(restriction.isAssignableFromType(relation.getDataTypeInformation())) {
        return (Relation<O>) relation;
      }
    }
    List<TypeInformation> types = new ArrayList<TypeInformation>(relations.size());
    for(Relation<?> relation : relations) {
      types.add(relation.getDataTypeInformation());
    }
    throw new NoSupportedDataTypeException(restriction, types);
  }

  @Override
  public <O, D extends Distance<D>> DistanceQuery<O, D> getDistanceQuery(Relation<O> objQuery, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("Distance query requested for 'null' distance!");
    }
    return distanceFunction.instantiate(objQuery);
  }

  @Override
  public <O, D extends Distance<D>> SimilarityQuery<O, D> getSimilarityQuery(Relation<O> objQuery, SimilarityFunction<? super O, D> similarityFunction, Object... hints) {
    if(similarityFunction == null) {
      throw new AbortException("Similarity query requested for 'null' similarity!");
    }
    return similarityFunction.instantiate(objQuery);
  }

  @Override
  public <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    ListIterator<Index> iter = indexes.listIterator(indexes.size());
    while(iter.hasPrevious()) {
      Index idx = iter.previous();
      if(idx instanceof KNNIndex) {
        if(getLogger().isDebuggingFinest()) {
          getLogger().debugFinest("Considering index for kNN Query: " + idx);
        }
        @SuppressWarnings("unchecked")
        final KNNIndex<O> knnIndex = (KNNIndex<O>) idx;
        KNNQuery<O, D> q = knnIndex.getKNNQuery(distanceQuery, hints);
        if(q != null) {
          return q;
        }
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    return QueryUtil.getLinearScanKNNQuery(distanceQuery);
  }

  @Override
  public <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    ListIterator<Index> iter = indexes.listIterator(indexes.size());
    while(iter.hasPrevious()) {
      Index idx = iter.previous();
      if(idx instanceof RangeIndex) {
        if(getLogger().isDebuggingFinest()) {
          getLogger().debugFinest("Considering index for range query: " + idx);
        }
        @SuppressWarnings("unchecked")
        final RangeIndex<O> rangeIndex = (RangeIndex<O>) idx;
        RangeQuery<O, D> q = rangeIndex.getRangeQuery(distanceQuery, hints);
        if(q != null) {
          return q;
        }
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    return QueryUtil.getLinearScanRangeQuery(distanceQuery);
  }

  @Override
  public <O, D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    ListIterator<Index> iter = indexes.listIterator(indexes.size());
    while(iter.hasPrevious()) {
      Index idx = iter.previous();
      if(idx instanceof RKNNIndex) {
        if(getLogger().isDebuggingFinest()) {
          getLogger().debugFinest("Considering index for RkNN Query: " + idx);
        }
        @SuppressWarnings("unchecked")
        final RKNNIndex<O> rknnIndex = (RKNNIndex<O>) idx;
        RKNNQuery<O, D> q = rknnIndex.getRKNNQuery(distanceQuery, hints);
        if(q != null) {
          return q;
        }
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
    KNNQuery<O, D> knnQuery = getKNNQuery(distanceQuery, DatabaseQuery.HINT_BULK, maxk);
    return new LinearScanRKNNQuery<O, D>(distanceQuery, knnQuery, maxk);
  }

  @Override
  public void addDataStoreListener(DataStoreListener l) {
    eventManager.addListener(l);
  }

  @Override
  public void removeDataStoreListener(DataStoreListener l) {
    eventManager.removeListener(l);
  }

  @Override
  public void accumulateDataStoreEvents() {
    eventManager.accumulateDataStoreEvents();
  }

  @Override
  public void flushDataStoreEvents() {
    eventManager.flushDataStoreEvents();
  }

  @Override
  public String getLongName() {
    return "Database";
  }

  @Override
  public String getShortName() {
    return "database";
  }

  abstract protected Logging getLogger();
}