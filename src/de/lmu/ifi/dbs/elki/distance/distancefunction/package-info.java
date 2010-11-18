/**
 * <p>Distance functions for use within ELKI.</p>
 * 
 * <h1>Distance functions</h1>
 * <p>There are three basic types of distance functions:</p>
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction Primitive Distance Function}s that can be computed for any two objects.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.distancefunction.DBIDDistanceFunction DBID Distance Function}s, that are only defined for object IDs, e.g. an external distance matrix</li>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.distancefunction.PreprocessorBasedDistanceFunction Preprocessor-Based Distance Function}s, that require a preprocessing step, and are then valid for existing database objects.</li>
 * </ul>
 * These types differ significantly in both implementation and use.</p>
 * 
 * <h1>Using distance functions</h1>
 * 
 * <p>As a 'consumer' of distances, you usually do not care about the type of distance function you
 * want to use. To facilitate this, a distance function can be <em>bound to a database</em> by calling
 * the 'instantiate' method to obtain a {@link de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery DistanceQuery} object.
 * A distance query is a best-effort adapter for the given distance function. Usually, you pass it
 * two DBIDs and get the distance value back. When required, the adapter will get the appropriate
 * records from the database needed to compute the distance.</p>
 * 
 * <p>Note: instantiating a preprocessor based distance will <em>invoke</em> the preprocessing step.
 * It is recommended to do this as soon as possible, and only instantiate the query <em>once</em>,
 * then pass the query object through the various methods.</p>
 * 
 * <h2>Code example</h2>
 * <pre>{@code
 * DistanceQuery<V, DoubleDistance> distanceQuery = database.getDistanceQuery(EuclideanDistanceFunction.STATIC);
 * }</pre>
 * 
 */
package de.lmu.ifi.dbs.elki.distance.distancefunction;

