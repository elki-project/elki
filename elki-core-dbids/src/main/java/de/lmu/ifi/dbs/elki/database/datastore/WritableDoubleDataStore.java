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
 * Data store specialized for doubles. Avoids boxing/unboxing.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public interface WritableDoubleDataStore extends DoubleDataStore, WritableDataStore<Double> {
  /**
   * Setter, but using objects.
   * 
   * @deprecated Use {@link #putDouble} instead, to avoid boxing/unboxing cost.
   */
  @Override
  @Deprecated
  Double put(DBIDRef id, Double value);

  /**
   * Associates the specified value with the specified id in this storage. If
   * the storage previously contained a value for the id, the previous value is
   * replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  double putDouble(DBIDRef id, double value);

  /**
   * Associates the specified value with the specified id in this storage. If
   * the storage previously contained a value for the id, the previous value is
   * replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  double put(DBIDRef id, double value);

  /**
   * Increment the specified value with the specified id in this storage.
   * 
   * @param id Database ID.
   * @param value Value to add to the previous value.
   */
  void increment(DBIDRef id, double value);
}