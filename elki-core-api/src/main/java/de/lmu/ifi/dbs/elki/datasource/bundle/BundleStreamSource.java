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
package de.lmu.ifi.dbs.elki.datasource.bundle;

import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
/**
 * Soruce for a bundle stream
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @composed - - - BundleMeta
 * @has - - - BundleStreamSource.Event
 */
public interface BundleStreamSource {
  /**
   * Events
   * 
   * @author Erich Schubert
   */
  enum Event {
    // Metadata has changed
    META_CHANGED,
    // Next object available
    NEXT_OBJECT,
    // Stream ended
    END_OF_STREAM,
  };

  /**
   * Get the current meta data.
   * 
   * @return Metadata
   */
  BundleMeta getMeta();

  /**
   * Access a particular object and representation.
   * 
   * @param rnum Representation number
   * @return Contained data
   */
  Object data(int rnum);

  /**
   * Indicate whether the stream contains DBIDs.
   * 
   * @return {@code true} if the stream contains DBIDs.
   */
  boolean hasDBIDs();

  /**
   * Assign the current object ID to a {@link DBIDVar}.
   * 
   * @param var Variable to assign the object id to
   * @return {@code false} when no object id is available
   */
  boolean assignDBID(DBIDVar var);

  /**
   * Get the next event
   * 
   * @return Event type
   */
  Event nextEvent();

  /**
   * Return (or collect) the stream as bundle.
   * 
   * @return Bundle
   */
  MultipleObjectsBundle asMultipleObjectsBundle();
}