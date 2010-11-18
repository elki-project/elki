/**
 * <p><b>Database queries</b> - computing distances, neighbors, similarities - API and general documentation.</p>
 * 
 * <h2>Introduction</h2>
 * 
 * <p>The database query API is designed around the concept of <b>prepared statements</b>.</p>
 * <p>When working with index structures, preprocessors, caches and external data, computing
 * a distance or a neighborhood is not as simple as running a constant time function.
 * Some functions may only be defined on a subset of the data, others can be computed much
 * more efficiently by performing a batch operation. When plenty of memory is available,
 * caching can be faster than recomputing distances all the time. And often there will be more
 * than one way of computing the same data
 * (for example by using an index or doing a linear scan).</p>
 *
 * <p>Usually, these operations are invoked very often.
 * Even deciding which method to use at every iteration can prove quite costly when the number
 * of iterations becomes large. Therefore the goal is to "optimize" once, then invoke the same
 * handler cheaply. This can be achieved by using "prepared statements" as this would be called
 * in a traditional RDBMS context.</p>
 * 
 * <h2>Prepared queries in ELKI</h2>
 * 
 * <p>Prepared statements in ELKI are currently available for:
 * <ul>
 * <li>Distance queries: {@link de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery DistanceQuery}</li>
 * <li>Similarity queries: {@link de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery SimilarityQuery}</li>
 * <li>kNN (k-nearest-neighbors) queries: {@link de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery KNNQuery}</li>
 * <li>&epsilon;-range queries: {@link de.lmu.ifi.dbs.elki.database.query.range.RangeQuery RangeQuery}</li>
 * <li>rkNN (reverse k-nearest-neighbors) queries: {@link de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery RKNNQuery}</li>
 * </ul>
 * with a quite similar API.</p>
 * 
 * <h2>Obtaining query objects</h2>
 * 
 * <p>The general process of obtaining a Query is to retrieve it from the database using:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getDistanceQuery Database.getDistanceQuery(distance)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getSimilarityQuery Database.getSimilarityQuery(similarity)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getKNNQuery Database.getKNNQuery(distance)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getRangeQuery Database.getRangeQuery(distance)}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.Database#getRKNNQuery Database.getRKNNQuery(distance)}</li>
 * </ul>
 * as appropriate. See the query class links above for the detailed API. Avoid calling this method within a loop construct!</p>
 * 
 * <p>The query can then be evaluated on objects as needed.</p>
 * 
 * <h2>Optimizer hints</h2>
 * 
 * <p>In order to assist the database layer to choose the most suitable implementation, one should
 * also provide so called "hints" as available. In general, any object could be a "hint" to the
 * database layer (for extensibility), but the following are commonly used:</p>
 * 
 * <ul>
 * <li>An Integer as maximum value of "k" used in kNN and rkNN queries
 * (since a preprocessor or index might only support a certain fixed maximum value)</li>
 * <li>A maximum distance used in range queries</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK HINT_BULK} to request support for bulk operations</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_EXACT HINT_EXACT} to exclude approximate answers</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_HEAVY_USE HINT_HEAVY_USE} to suggest the use of a cache or preprocessor</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_OPTIMIZED_ONLY HINT_OPTIMIZED_ONLY} to disallow linear scans</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_SINGLE HINT_SINGLE} to disallow expensive optimizations, since the query will only be used once</li>
 * </ul>
 * 
 * <p>Please set these hints appropriately, since this can effect your algorithms performance!</p>
 * 
 * <h2>Full example:</h2>
 * 
 * <blockquote><pre> {@code // Get a kNN query with maxk = 10
 * KNNQuery<V, DoubleDistance> knnQuery = database.getKNNQuery(EuclideanDistanceFunction.STATIC, 10);
 * // run a 10NN query for each point, discarding the results
 * for(DBID id : database) {
 *   knnQuery.getKNNForDBID(id, 10);
 * }
 * }</pre></blockquote>
 */
package de.lmu.ifi.dbs.elki.database.query;

