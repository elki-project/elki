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

import java.util.function.BiConsumer;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;

/**
 * An object representation from a database.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - DBIDRef
 * @opt hide TooltipStringVisualization
 *
 * @param <O> Object type
 */
public interface Relation<O> extends DatabaseQuery, HierarchicalResult {
  /**
   * Get the representation of an object.
   *
   * @param id Object ID
   * @return object instance
   */
  O get(DBIDRef id);

  /**
   * Get the data type of this representation
   *
   * @return Data type
   */
  SimpleTypeInformation<O> getDataTypeInformation();

  /**
   * Get the IDs the query is defined for.
   *
   * If possible, prefer {@link #iterDBIDs()}.
   *
   * @return IDs this is defined for
   */
  DBIDs getDBIDs();

  /**
   * Get an iterator access to the DBIDs.
   *
   * To iterate over all IDs, use the following code fragment:
   *
   * <pre>
   * {@code
   * for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
   *    relation.get(iter); // Get the current element
   * }
   * }
   * </pre>
   *
   * @return iterator for the DBIDs.
   */
  DBIDIter iterDBIDs();

  /**
   * Get the number of DBIDs.
   *
   * @return Size
   */
  int size();

  /**
   * Execute a function for each ID.
   *
   * @param action Action to execute
   */
  default void forEach(BiConsumer<? super DBIDRef, ? super O> action) {
    for(DBIDIter it = iterDBIDs(); it.valid(); it.advance()) {
      action.accept(it, get(it));
    }
  }

  /**
   * Get the distance query for a particular distance function.
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return Instance to query the database with this distance
   */
  DistanceQuery<O> getDistanceQuery(DistanceFunction<? super O> distanceFunction, Object... hints);

  /**
   * Get the similarity query for a particular similarity function.
   *
   * @param similarityFunction Similarity function to use
   * @param hints Optimizer hints (optional)
   * @return Instance to query the database with this similarity
   */
  SimilarityQuery<O> getSimilarityQuery(SimilarityFunction<? super O> similarityFunction, Object... hints);

  /**
   * Get a KNN query object for the given distance query.
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param distanceQuery Distance query
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a KNN query object for the given distance query.
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default KNNQuery<O> getKNNQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    DistanceQuery<O> distanceQuery = getDistanceQuery(distanceFunction, hints);
    return getKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a range query object for the given distance query. (Range queries in
   * ELKI refers to radius-based ranges, not rectangular query windows.)
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param distanceQuery Distance query
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a range query object for the given distance query. (Range queries in
   * ELKI refers to radius-based ranges, not rectangular query windows.)
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default RangeQuery<O> getRangeQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    DistanceQuery<O> distanceQuery = getDistanceQuery(distanceFunction, hints);
    return getRangeQuery(distanceQuery, hints);
  }

  /**
   * Get a range query object for the given similarity query. (Range queries in
   * ELKI refers to radius-based ranges, not rectangular query windows.)
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param simQuery Similarity query
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  RangeQuery<O> getSimilarityRangeQuery(SimilarityQuery<O> simQuery, Object... hints);

  /**
   * Get a range query object for the given similarity query. (Range queries in
   * ELKI refers to radius-based ranges, not rectangular query windows.)
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Double: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param simFunction Similarity function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default RangeQuery<O> getSimilarityRangeQuery(SimilarityFunction<? super O> simFunction, Object... hints) {
    SimilarityQuery<O> simQuery = getSimilarityQuery(simFunction, hints);
    return getSimilarityRangeQuery(simQuery, hints);
  }

  /**
   * Get a rKNN query object for the given distance query.
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param distanceQuery Distance query
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a rKNN query object for the given distance query.
   *
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_BULK} bulk query needed</li>
   * </ul>
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default RKNNQuery<O> getRKNNQuery(DistanceFunction<? super O> distanceFunction, Object... hints) {
    DistanceQuery<O> distanceQuery = getDistanceQuery(distanceFunction, hints);
    return getRKNNQuery(distanceQuery, hints);
  }
}