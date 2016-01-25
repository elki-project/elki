package de.lmu.ifi.dbs.elki.database.ids;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;

/**
 * (Persistent) variable storing a DBID reference.
 *
 * In contrast to the {@link DBIDRef} API, which are read-only references, this
 * variable can be updated to point to a different DBID, e.g. the current best
 * candidate.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface DBIDVar extends DBIDRef, ArrayDBIDs, SetDBIDs {
  /**
   * Assign a new value for the reference.
   *
   * @param ref Reference
   */
  void set(DBIDRef ref);

  /**
   * Update variable from a data store.
   *
   * @param store Data store
   * @param ref Reference
   * @return Self, for inlining.
   */
  DBIDVar from(DBIDDataStore store, DBIDRef ref);

  /**
   * Clear the contents.
   */
  void unset();

  /**
   * Test if the variable is well-defined.
   *
   * @return {@code true} when assigned.
   */
  boolean isSet();

  /**
   * Assign the first pair member to this variable.
   *
   * @param pair Pair
   */
  void setFirst(DBIDPair pair);

  /**
   * Assign the second pair member to this variable.
   *
   * @param pair Pair
   */
  void setSecond(DBIDPair pair);
}
