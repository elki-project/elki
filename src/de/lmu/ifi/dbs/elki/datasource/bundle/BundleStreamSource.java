package de.lmu.ifi.dbs.elki.datasource.bundle;

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

/**
 * Soruce for a bundle stream
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf BundleMeta
 */
public interface BundleStreamSource {
  /**
   * Events
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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
   * Get the next event
   * 
   * @return Event type
   */
  public Event nextEvent();
}