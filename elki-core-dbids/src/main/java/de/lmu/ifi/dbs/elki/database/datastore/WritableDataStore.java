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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Writable data store.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @param <T> Data type
 */
public interface WritableDataStore<T> extends DataStore<T> {
  /**
   * Associates the specified value with the specified id in this storage. If
   * the storage previously contained a value for the id, the previous value is
   * replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  T put(DBIDRef id, T value);

  /**
   * Deallocate the storage, freeing the memory and notifies the registered
   * listeners.
   */
  void destroy();

  /**
   * Delete the contents for a particular ID and notifies the registered
   * listeners.
   * 
   * @param id Database ID.
   */
  void delete(DBIDRef id);

  /**
   * Clear the storage (resetting it to the default value).
   */
  void clear();
}
