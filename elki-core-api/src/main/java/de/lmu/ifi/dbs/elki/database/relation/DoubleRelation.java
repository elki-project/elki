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
package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Interface for double-valued relations.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface DoubleRelation extends ModifiableRelation<Double> {
  /**
   * Get the representation of an object.
   *
   * @param id Object ID
   * @return object instance
   */
  double doubleValue(DBIDRef id);

  /**
   * @deprecated use {@link #doubleValue} instead.
   */
  @Deprecated
  @Override
  default Double get(DBIDRef id) {
    return doubleValue(id);
  }

  /**
   * Set an object representation.
   *
   * @param id Object ID
   * @param val Value
   */
  // TODO: remove / move to a writable API?
  void set(DBIDRef id, double val);

  /**
   * @deprecated use {@link #set(DBIDRef, double)} instead.
   */
  @Deprecated
  @Override
  void insert(DBIDRef id, Double val);

  /**
   * Execute a function for each ID.
   *
   * @param action Action to execute
   */
  default void forEachDouble(Consumer action) {
    for(DBIDIter it = iterDBIDs(); it.valid(); it.advance()) {
      action.accept(it, doubleValue(it));
    }
  }

  /**
   * Consumer for (DBIDRef, double) pairs.
   *
   * @author Erich Schubert
   * 
   * @assoc - iterates - DoubleRelation
   */
  @FunctionalInterface
  interface Consumer {
    /**
     * Act on each value.
     *
     * @param idref DBID reference
     * @param val value
     */
    void accept(DBIDRef idref, double val);
  }
}
