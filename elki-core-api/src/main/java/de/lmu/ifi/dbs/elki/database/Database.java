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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;

/**
 * Database specifies the requirements for any database implementation. Note
 * that any implementing class is supposed to provide a constructor without
 * parameters for dynamic instantiation.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @navhas - provides - DistanceQuery
 * @navhas - provides - KNNQuery
 * @navhas - provides - RangeQuery
 * @navhas - provides - RKNNQuery
 * @navhas - contains - Relation
 * @navhas - invokes - DataStoreListener
 */
public interface Database extends HierarchicalResult {
  /**
   * Initialize the database, for example by loading the input data. (Since this
   * should NOT be done on construction time!)
   */
  void initialize();

  /**
   * Get all relations of a database.
   *
   * @return All relations in the database
   */
  Collection<Relation<?>> getRelations();

  /**
   * Get an object representation.
   *
   * @param <O> Object type
   * @param restriction Type restriction
   * @param hints Optimizer hints
   * @return representation
   */
  <O> Relation<O> getRelation(TypeInformation restriction, Object... hints) throws NoSupportedDataTypeException;

  /**
   * Get the distance query for a particular distance function.
   *
   * @param <O> Object type
   * @param relation Relation used
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints
   * @return Instance to query the database with this distance
   */
  <O> DistanceQuery<O> getDistanceQuery(Relation<O> relation, DistanceFunction<? super O> distanceFunction, Object... hints);

  /**
   * Get the similarity query for a particular similarity function.
   *
   * @param <O> Object type
   * @param relation Relation used
   * @param similarityFunction Similarity function to use
   * @param hints Optimizer hints
   * @return Instance to query the database with this similarity
   */
  <O> SimilarityQuery<O> getSimilarityQuery(Relation<O> relation, SimilarityFunction<? super O> similarityFunction, Object... hints);

  /**
   * Get a KNN query object for the given distance query.
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param <O> Object type
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O> KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a range query object for the given distance query for radius-based
   * neighbor search. (Range queries in ELKI refers to radius-based ranges, not
   * rectangular query windows.)
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Double: Maximum query range that will be used.</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param <O> Object type
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O> RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a range query object for the given distance query for radius-based
   * neighbor search. (Range queries in ELKI refers to radius-based ranges, not
   * rectangular query windows.)
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Double: Minimum query similarity that will be used.</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param <O> Object type
   * @param simQuery Similarity query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O> RangeQuery<O> getSimilarityRangeQuery(SimilarityQuery<O> simQuery, Object... hints);

  /**
   * Get a rKNN query object for the given distance query.
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param <O> Object type
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O> RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Returns the DatabaseObject represented by the specified id.
   *
   * @param id the id of the Object to be obtained from the Database
   * @return Bundle containing the objects' data
   */
  SingleObjectBundle getBundle(DBIDRef id);

  // TODO: add
  /*
   * Returns the DatabaseObject represented by the specified id.
   *
   * @param id the id of the Object to be obtained from the Database
   * @return Bundle containing the object data
   * @throws ObjectNotFoundException when the DBID was not found.
   */
  // MultipleObjectsBundle getBundles(DBIDs id) throws ObjectNotFoundException;

  /**
   * Adds a listener for the <code>DataStoreEvent</code> posted after the
   * content of the database changes.
   *
   * @param l the listener to add
   * @see #removeDataStoreListener(DataStoreListener)
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  void addDataStoreListener(DataStoreListener l);

  /**
   * Removes a listener previously added with
   * {@link #addDataStoreListener(DataStoreListener)}.
   *
   * @param l the listener to remove
   * @see #addDataStoreListener(DataStoreListener)
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  void removeDataStoreListener(DataStoreListener l);

  /**
   * Collects all insertion, deletion and update events until
   * {@link #flushDataStoreEvents()} is called.
   *
   * @see DataStoreEvent
   */
  void accumulateDataStoreEvents();

  /**
   * Fires all collected insertion, deletion and update events as one
   * DataStoreEvent, i.e. notifies all registered DataStoreListener how the
   * content of the database has been changed since
   * {@link #accumulateDataStoreEvents()} has been called.
   *
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  void flushDataStoreEvents();
}
