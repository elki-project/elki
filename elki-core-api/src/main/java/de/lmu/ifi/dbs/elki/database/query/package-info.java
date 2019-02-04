/**
 * <b>Database queries</b> - computing distances, neighbors, similarities - API
 * and general documentation
 * <h2>Introduction</h2>
 * The database query API is designed around the concept of <b>prepared
 * statements</b>.
 * <p>
 * When working with index structures, preprocessors, caches and external data,
 * computing a distance or a neighborhood is not as simple as running a constant
 * time function. Some functions may only be defined on a subset of the data,
 * others can be computed much more efficiently by performing a batch operation.
 * When plenty of memory is available, caching can be faster than recomputing
 * distances all the time. And often there will be more than one way of
 * computing the same data (for example by using an index or doing a linear
 * scan).
 * <p>
 * Usually, these operations are invoked very often. Even deciding which method
 * to use at every iteration can prove quite costly when the number of
 * iterations becomes large. Therefore the goal is to "optimize" once, then
 * invoke the same handler cheaply. This can be achieved by using "prepared
 * statements" as this would be called in a traditional RDBMS context.
 * <h2>Prepared queries in ELKI</h2>
 * Prepared statements in ELKI are currently available for:
 * <ul>
 * <li>Distance queries:
 * {@link de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery
 * DistanceQuery}</li>
 * <li>Similarity queries:
 * {@link de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery
 * SimilarityQuery}</li>
 * <li>kNN (k-nearest-neighbors) queries:
 * {@link de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery KNNQuery}</li>
 * <li>&epsilon;-range queries:
 * {@link de.lmu.ifi.dbs.elki.database.query.range.RangeQuery RangeQuery}</li>
 * <li>rkNN (reverse k-nearest-neighbors) queries:
 * {@link de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery RKNNQuery}</li>
 * </ul>
 * with a quite similar API.
 * <h2>Obtaining query objects</h2>
 * The general process of obtaining a Query is to retrieve it from the database
 * using:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getDistanceQuery
 * Database.getDistanceQuery(distance)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getSimilarityQuery
 * Database.getSimilarityQuery(similarity)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getKNNQuery
 * Database.getKNNQuery(distance)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getRangeQuery
 * Database.getRangeQuery(distance)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getRKNNQuery
 * Database.getRKNNQuery(distance)}</li>
 * </ul>
 * as appropriate. See the query class links above for the detailed API. Avoid
 * calling this method within a loop construct!<br>
 * The query can then be evaluated on objects as needed.
 * <h2>Optimizer hints</h2>
 * In order to assist the database layer to choose the most suitable
 * implementation, one should also provide so called "hints" as available. In
 * general, any object could be a "hint" to the database layer (for
 * extensibility), but the following are commonly used:
 * <ul>
 * <li>An Integer as maximum value of "k" used in kNN and rkNN queries
 * (since a preprocessor or index might only support a certain fixed maximum
 * value)</li>
 * <li>A maximum distance used in range queries</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK
 * DatabaseQuery.HINT_BULK} to request support for bulk operations</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_EXACT
 * DatabaseQuery.HINT_EXACT} to exclude approximate answers</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_HEAVY_USE
 * DatabaseQuery.HINT_HEAVY_USE} to suggest the use of a cache or
 * preprocessor</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_OPTIMIZED_ONLY
 * DatabaseQuery.HINT_OPTIMIZED_ONLY} to disallow linear scans</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_SINGLE
 * DatabaseQuery.HINT_SINGLE} to disallow expensive optimizations, since the
 * query will only be used once</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_NO_CACHE
 * DatabaseQuery.HINT_NO_CACHE} to disallow retrieving a cache class</li>
 * </ul>
 * Please set these hints appropriately, since this can effect your algorithms
 * performance!
 * <h2>Full example:</h2>
 * 
 * <pre>
 * // Get a kNN query with maxk = 10
 * KNNQuery&lt;V, DoubleDistance&gt; knnQuery = database.getKNNQuery(EuclideanDistanceFunction.STATIC, 10);
 * // run a 10NN query for each point, discarding the results
 * for(DBID id : database) {
 *   knnQuery.getKNNForDBID(id, 10);
 * }
 * </pre>
 *
 * @opt include .*elki.database.query.distance.DistanceQuery
 * @opt include .*elki.database.query.similarity.SimilarityQuery
 * @opt include .*elki.database.query.knn.KNNQuery
 * @opt include .*elki.database.query.rknn.RKNNQuery
 * @opt include .*elki.database.query.range.RangeQuery
 */
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
package de.lmu.ifi.dbs.elki.database.query;
