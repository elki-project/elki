package de.lmu.ifi.dbs.elki.datasource.bundle;

import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

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

/**
 * Soruce for a bundle stream
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.composedOf BundleMeta
 * @apiviz.has BundleStreamSource.Event
 */
public interface BundleStreamSource {
  /**
   * Events
   * 
   * @author Erich Schubert
   */
  public static enum Event {
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
  public BundleMeta getMeta();

  /**
   * Access a particular object and representation.
   * 
   * @param rnum Representation number
   * @return Contained data
   */
  public Object data(int rnum);

  /**
   * Indicate whether the stream contains DBIDs.
   * 
   * @return {@code true} if the stream contains DBIDs.
   */
  public boolean hasDBIDs();

  /**
   * Assign the current object ID to a {@link DBIDVar}.
   * 
   * @param var Variable to assign the object id to
   * @return {@code false} when no object id is available
   */
  public boolean assignDBID(DBIDVar var);

  /**
   * Get the next event
   * 
   * @return Event type
   */
  public Event nextEvent();

  /**
   * Return (or collect) the stream as bundle.
   * 
   * @return Bundle
   */
  public MultipleObjectsBundle asMultipleObjectsBundle();
}