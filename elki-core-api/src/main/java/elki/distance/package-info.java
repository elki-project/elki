/**
 * Distance functions for use within ELKI.
 * <p>
 * <h1>Distance functions</h1>
 * <p>There are three basic types of distance functions:
 * <ul>
 * <li>{@link elki.distance.PrimitiveDistance Primitive Distance Function}s that can be computed for any two objects.</li>
 * <li>{@link elki.distance.DBIDDistance DBID Distance Function}s, that are only defined for object IDs, e.g. an external distance matrix</li>
 * <li>{@link elki.distance.IndexBasedDistance Index-Based Distance Function}s, that require an indexing/preprocessing step, and are then valid for existing database objects.</li>
 * </ul>
 * These types differ significantly in both implementation and use.
 * <p>
 * <h1>Using distance functions</h1>
 * <p>
 * As a 'consumer' of distances, you usually do not care about the type of distance function you
 * want to use. To facilitate this, a distance function can be <em>bound to a database</em> by calling
 * the 'instantiate' method to obtain a {@link elki.database.query.distance.DistanceQuery DistanceQuery} object.
 * A distance query is a best-effort adapter for the given distance function. Usually, you pass it
 * two DBIDs and get the distance value back. When required, the adapter will get the appropriate
 * records from the database needed to compute the distance.
 * <p>
 * Note: instantiating a preprocessor based distance will <em>invoke</em> the preprocessing step.
 * It is recommended to do this as soon as possible, and only instantiate the query <em>once</em>,
 * then pass the query object through the various methods.
 * <p>
 * <h2>Code example</h2>
 * {@code
 * DistanceQuery<V> distanceQuery = database.getDistanceQuery(EuclideanDistance.STATIC);
 * }
 * 
 * @opt hide elki.application.*
 * @opt hide elki.algorithm.*
 * @opt hide elki.database.*
 * @opt hide elki.distance\..*\.
 * @opt hide elki.index.*
 * @opt hide elki.*Instance
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
package elki.distance;
