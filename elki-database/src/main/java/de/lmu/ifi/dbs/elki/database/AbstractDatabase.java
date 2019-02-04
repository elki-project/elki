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
package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract base class for database API implementations. Provides default
 * management of relations, indexes and events as well as default query
 * matching.
 * <p>
 * Note: when debugging index usage, set logging for this package to FINEST via
 * <tt>-enableDebug de.lmu.ifi.dbs.elki.database=FINEST</tt>
 *
 * @author Erich Schubert
 * @since 0.1
 * 
 * @composed - - - DatabaseEventManager
 * @has - - - IndexFactory
 */
public abstract class AbstractDatabase extends AbstractHierarchicalResult implements Database {
  /**
   * The event manager, collects events and fires them on demand.
   */
  protected final DatabaseEventManager eventManager = new DatabaseEventManager();

  /**
   * The relations we manage.
   */
  protected final List<Relation<?>> relations = new ArrayList<>();

  /**
   * Index factories.
   */
  protected final Collection<IndexFactory<?>> indexFactories = new ArrayList<>();

  /**
   * Constructor.
   */
  public AbstractDatabase() {
    super();
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
    List<TypeInformation> types = new ArrayList<>(relations.size());
    for(Relation<?> relation : relations) {
      types.add(relation.getDataTypeInformation());
    }
    throw new NoSupportedDataTypeException(restriction, types);
  }

  @Override
  public <O> DistanceQuery<O> getDistanceQuery(Relation<O> objQuery, DistanceFunction<? super O> distanceFunction, Object... hints) {
    return objQuery.getDistanceQuery(distanceFunction, hints);
  }

  @Override
  public <O> SimilarityQuery<O> getSimilarityQuery(Relation<O> objQuery, SimilarityFunction<? super O> similarityFunction, Object... hints) {
    return objQuery.getSimilarityQuery(similarityFunction, hints);
  }

  @Override
  public <O> KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    @SuppressWarnings("unchecked")
    final Relation<O> relation = (Relation<O>) distanceQuery.getRelation();
    return relation.getKNNQuery(distanceQuery, hints);
  }

  @Override
  public <O> RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    @SuppressWarnings("unchecked")
    final Relation<O> relation = (Relation<O>) distanceQuery.getRelation();
    return relation.getRangeQuery(distanceQuery, hints);
  }

  @Override
  public <O> RangeQuery<O> getSimilarityRangeQuery(SimilarityQuery<O> simQuery, Object... hints) {
    @SuppressWarnings("unchecked")
    final Relation<O> relation = (Relation<O>) simQuery.getRelation();
    return relation.getSimilarityRangeQuery(simQuery, hints);
  }

  @Override
  public <O> RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    @SuppressWarnings("unchecked")
    final Relation<O> relation = (Relation<O>) distanceQuery.getRelation();
    return relation.getRKNNQuery(distanceQuery, hints);
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

  /**
   * Get the class logger.
   *
   * @return Class logger
   */
  protected abstract Logging getLogger();

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Option to specify the data source for the database.
     */
    public static final OptionID DATABASE_CONNECTION_ID = new OptionID("dbc", "Database connection class.");

    /**
     * Parameter to specify the indexes to use.
     */
    public static final OptionID INDEX_ID = new OptionID("db.index", "Database indexes to add.");

    @Override
    protected abstract Database makeInstance();
  }
}
