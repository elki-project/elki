/**
 * General data store layer <em>API</em> (along the lines of
 * {@code Map<DBID, T>} - use everywhere!) for ELKI.
 * <p>
 * <h2>When to use:</h2>
 * <p>
 * Every time you need to associate a larger number of objects (in form of
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBID DBID}s) with any kind of value.
 * This can be temporary values such as KNN lists, but also result values such
 * as outlier scores.
 * <p>
 * Basically, {@code DataStore<T> == Map<DBID, T>}. But this API will allow
 * extensions that can do on-disk maps.
 * <p>
 * <h2>How to use:</h2>
 * {@code
 * // Storage for the outlier score of each ID. 
 * final WritableDoubleDataStore scores = DataStoreFactory.FACTORY.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
 * }
 * 
 * @opt hide datastore.memory
 * @opt hide index.preprocessed
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
package de.lmu.ifi.dbs.elki.database.datastore;