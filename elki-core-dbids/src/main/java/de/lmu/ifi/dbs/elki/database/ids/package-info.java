/**
 * Database object identification and ID group handling <em>API</em>.
 * <p>
 * Database IDs (short: DBID) in ELKI are based on the factory pattern, to allow replacing
 * the simple Integer-based DBIDs with more complex implementations, e.g. for use with external
 * databases or to add tracking for debugging purposes. This also allows adding of more efficient
 * implementations later on in a single place.
 * <p>
 * <h2>DBID interface:</h2>
 * <p>
 * The {@link de.lmu.ifi.dbs.elki.database.ids.DBID DBID} object identifies a single object.
 * <p>
 * The {@link de.lmu.ifi.dbs.elki.database.ids.DBIDs DBIDs} hierarchy contains classes for handling groups (sets, arrays) of IDs, that can
 * be seen as a two-dimensional matrix consisting 
 * <table style="border: 1px">
 * <tr>
 * <th></th>
 * <th style="border-bottom: 2px">{@link de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs ArrayDBIDs}</th>
 * <th style="border-bottom: 2px">{@link de.lmu.ifi.dbs.elki.database.ids.HashSetDBIDs HashSetDBIDs}</th>
 * </tr>
 * <tr>
 * <th style="border-right: 2px">{@link de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs ModifiableDBIDs}</th>
 * <td>{@link de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs ArrayModifiableDBIDs}</td>
 * <td>{@link de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs HashSetModifiableDBIDs}</td>
 * </tr>
 * <tr>
 * <th style="border-right: 2px">{@link de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs StaticDBIDs}</th>
 * <td>{@link de.lmu.ifi.dbs.elki.database.ids.ArrayStaticDBIDs ArrayStaticDBIDs}</td>
 * <td>n/a</td>
 * </tr>
 * </table>
 * <p>
 * {@link de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs StaticDBIDs} are structures that cannot support
 * modifications, but thus can be implemented more efficiently, for example as Interval. They are
 * mostly used by the data sources.
 * <p>
 * These interfaces cannot be instantiated, obviously. Instead, use the static
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBIDFactory#FACTORY DBIDFactory.FACTORY}, which is also wrapped in the {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil DBIDUtil} class.
 * <p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * DBIDs allids = database.getIDs();
 * // preallocate an array of initial capacity 123 
 * ArrayModifiableDBIDs array = DBIDUtil.newArraySet(123);
 * // new DBID hash set with minimum initial capacity
 * ModifiableDBIDs hash = DBIDUtil.newHashSet();
 * 
 * // add all DBIDs from the hash
 * tree.addDBIDs(hash)
 * }</pre>
 * <p>
 * <h2>Utility functions:</h2>
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#ensureArray DBIDUtil.ensureArray} to ensure {@link de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs ArrayDBIDs}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#ensureModifiable DBIDUtil.ensureModifiable} to ensure {@link de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs ModifiableDBIDS}</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#makeUnmodifiable DBIDUtil.makeUnmodifiable} to wrap DBIDs unmodifiable</li>
 * </ul>
 *
 * @opt hide de.lmu.ifi.dbs.elki.database.ids.DBIDFactory
 * @opt hide de.lmu.ifi.dbs.elki.database.ids.integer.*
 * @opt hide de.lmu.ifi.dbs.elki.database.ids.generic.*
 * @opt hide de.lmu.ifi.dbs.elki.database.ids.EmptyDBIDs.EmptyDBIDIterator
 * @opt hide de.lmu.ifi.dbs.elki.database.*Database
 * @opt hide de.lmu.ifi.dbs.elki.data.Cluster
 * @opt hide de.lmu.ifi.dbs.elki.datasource.filter.*
 * @opt hide de.lmu.ifi.dbs.elki.database.query.*
 * @opt hide de.lmu.ifi.dbs.elki.(algorithm|evaluation|parallel|distance|index|result|persistent|utilities).*
 * @opt hide de.lmu.ifi.dbs.elki.database.relation.*
 * @opt hide java.*
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
package de.lmu.ifi.dbs.elki.database.ids;
