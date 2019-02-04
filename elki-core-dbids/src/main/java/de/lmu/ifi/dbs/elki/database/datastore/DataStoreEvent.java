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

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Encapsulates information describing changes, i.e. updates, insertions, and /
 * or deletions in a {@link DataStore}, and used to notify all subscribed
 * {@link DataStoreListener} of the change.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @see DataStore
 * @see DataStoreListener
 */
public class DataStoreEvent {
  /**
   * Insertions.
   */
  private DBIDs inserts;

  /**
   * Removals.
   */
  private DBIDs removals;

  /**
   * Updates.
   */
  private DBIDs updates;

  /**
   * Constructor.
   *
   * @param inserts Insertions
   * @param removals Removals
   * @param updates Updates
   */
  public DataStoreEvent(DBIDs inserts, DBIDs removals, DBIDs updates) {
    super();
    this.inserts = inserts;
    this.removals = inserts;
    this.updates = inserts;
  }

  /**
   * Insertion event.
   *
   * @param inserts Insertions
   * @return Event
   */
  public static DataStoreEvent insertionEvent(DBIDs inserts) {
    return new DataStoreEvent(inserts, DBIDUtil.EMPTYDBIDS, DBIDUtil.EMPTYDBIDS);
  }

  /**
   * Removal event.
   *
   * @param removals Removal
   * @return Event
   */
  public static DataStoreEvent removalEvent(DBIDs removals) {
    return new DataStoreEvent(DBIDUtil.EMPTYDBIDS, removals, DBIDUtil.EMPTYDBIDS);
  }

  /**
   * Update event.
   *
   * @param updates Updates
   * @return Event
   */
  public static DataStoreEvent updateEvent(DBIDs updates) {
    return new DataStoreEvent(DBIDUtil.EMPTYDBIDS, DBIDUtil.EMPTYDBIDS, updates);
  }

  /**
   * Get the inserted objects.
   *
   * @return Insertions
   */
  public DBIDs getInserts() {
    return inserts;
  }

  /**
   * Get the removed objects.
   *
   * @return Removals
   */
  public DBIDs getRemovals() {
    return removals;
  }

  /**
   * Get the updates objects.
   *
   * @return Updates
   */
  public DBIDs getUpdates() {
    return updates;
  }
}