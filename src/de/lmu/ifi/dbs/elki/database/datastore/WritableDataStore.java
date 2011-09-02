package de.lmu.ifi.dbs.elki.database.datastore;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Writable data store.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @param <T>
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
  public T put(DBID id, T value);

  /**
   * Deallocate the storage, freeing the memory and notifies the registered
   * listeners.
   */
  public void destroy();

  /**
   * Delete the contents for a particular ID and notifies the registered
   * listeners.
   * 
   * @param id Database ID.
   */
  public void delete(DBID id);
}
