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
package elki.database.relation;

import java.util.function.BiConsumer;

import elki.data.type.SimpleTypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.DatabaseQuery;
import elki.database.query.distance.DistancePrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.query.rknn.RKNNQuery;
import elki.database.query.similarity.SimilarityQuery;
import elki.distance.Distance;
import elki.similarity.Similarity;

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
public interface Relation<O> extends DatabaseQuery {
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
   * <p>
   * If possible, prefer {@link #iterDBIDs()}.
   *
   * @return IDs this is defined for
   */
  DBIDs getDBIDs();

  /**
   * Get an iterator access to the DBIDs.
   * <p>
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
   * Get a long (human readable) name for the relation.
   *
   * @return Relation name
   */
  // @Override // Used to be in "Result"
  String getLongName();

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
  DistanceQuery<O> getDistanceQuery(Distance<? super O> distanceFunction, Object... hints);

  /**
   * Get the similarity query for a particular similarity function.
   *
   * @param similarityFunction Similarity function to use
   * @param hints Optimizer hints (optional)
   * @return Instance to query the database with this similarity
   */
  SimilarityQuery<O> getSimilarityQuery(Similarity<? super O> similarityFunction, Object... hints);

  /**
   * Get a KNN query object for the given distance query.
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distanceQuery Distance query
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a KNN query object for the given distance query.
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default KNNQuery<O> getKNNQuery(Distance<? super O> distanceFunction, Object... hints) {
    return getKNNQuery(getDistanceQuery(distanceFunction, hints), hints);
  }

  /**
   * Get a range query object for the given distance query. (Range queries in
   * ELKI refers to radius-based ranges, not rectangular query windows.)
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
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
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default RangeQuery<O> getRangeQuery(Distance<? super O> distanceFunction, Object... hints) {
    return getRangeQuery(getDistanceQuery(distanceFunction, hints), hints);
  }

  /**
   * Get a range query object for the given similarity query. (Range queries in
   * ELKI refers to radius-based ranges, not rectangular query windows.)
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
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
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Double: Maximum query range</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param simFunction Similarity function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default RangeQuery<O> getSimilarityRangeQuery(Similarity<? super O> simFunction, Object... hints) {
    return getSimilarityRangeQuery(getSimilarityQuery(simFunction, hints), hints);
  }

  /**
   * Get a rKNN query object for the given distance query.
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * quadratic scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distanceQuery Distance query
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a rKNN query object for the given distance query.
   * <p>
   * When possible, this will use an index, but it may default to an expensive
   * quadratic scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints (optional)
   * @return KNN Query object
   */
  default RKNNQuery<O> getRKNNQuery(Distance<? super O> distanceFunction, Object... hints) {
    return getRKNNQuery(getDistanceQuery(distanceFunction, hints), hints);
  }

  /**
   * Get a priority search object for the given distance query. It will
   * incrementally provide results in approximately ascending order.
   * <p>
   * An index is used when possible, but it may fall back to a linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distanceQuery Distance function
   * @param hints Optimizer hints
   * @return Priority searcher
   */
  DistancePrioritySearcher<O> getPrioritySearcher(DistanceQuery<O> distanceQuery, Object... hints);

  /**
   * Get a priority search object for the given distance function. It will
   * incrementally provide results in approximately ascending order.
   * <p>
   * An index is used when possible, but it may fall back to a linear scan.
   * <p>
   * Hints include:
   * <ul>
   * <li>{@link DatabaseQuery#HINT_EXACT} -- no approximative indexes</li>
   * <li>{@link DatabaseQuery#HINT_OPTIMIZED_ONLY} -- no linear scans</li>
   * <li>{@link DatabaseQuery#HINT_HEAVY_USE} -- recommend optimization</li>
   * <li>{@link DatabaseQuery#HINT_SINGLE} -- discourage expensive
   * optimization</li>
   * </ul>
   *
   * @param distance Distance function
   * @param hints Optimizer hints
   * @return Priority searcher
   */
  default DistancePrioritySearcher<O> getPrioritySearcher(Distance<O> distance, Object... hints) {
    return getPrioritySearcher(getDistanceQuery(distance, hints), hints);
  }
}
