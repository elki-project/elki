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

/**
 * Defines the interface for an object that listens to changes in a
 * {@link DataStore}.
 *
 * @author Elke Achtert
 * @since 0.4.0
 *
 * @navassoc - listens - DataStoreEvent
 *
 * @see DataStore
 * @see DataStoreEvent
 */
public interface DataStoreListener {
  /**
   * Invoked after objects of the datastore have been updated, inserted or
   * removed in some way.
   *
   * @param e the update event
   */
  void contentChanged(DataStoreEvent e);

  /**
   * Invoked after the data store has been destroyed.
   *
   * @param e the destroy event
   */
  //void dataStoreDestroyed(DataStoreEvent e);
}
