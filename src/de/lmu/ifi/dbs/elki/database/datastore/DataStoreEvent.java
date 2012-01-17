package de.lmu.ifi.dbs.elki.database.datastore;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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

import java.util.EventObject;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Encapsulates information describing changes, i.e. updates, insertions, and /
 * or deletions in a {@link DataStore}, and used to notify all subscribed
 * {@link DataStoreListener} of the change.
 * 
 * @author Elke Achtert
 * @see DataStore
 * @see DataStoreListener
 */
public class DataStoreEvent extends EventObject {
  /**
   * Serialization ID since Java EventObjects are expected to be serializable.
   */
  private static final long serialVersionUID = 7183716156466324055L;

  /**
   * Available event types.
   * 
   * @apiviz.exclude
   */
  public enum Type {
    /**
     * Identifies a change on existing objects.
     */
    UPDATE,

    /**
     * Identifies the insertion of new objects.
     */
    INSERT,

    /**
     * Identifies the removal of objects.
     */
    DELETE
  }

  /**
   * The objects that were changed in the {@link DataStore} mapped by the type
   * of change.
   */
  // FIXME: instead of a (costly) map, use just three DBIDs references?
  private final Map<Type, DBIDs> objects;

  /**
   * Used to create an event when objects have been updated in, inserted into,
   * and / or removed from the specified {@link DataStore}.
   * 
   * @param source the object responsible for generating the event
   * @param objects the objects that have been changed mapped by the type of
   *        change
   * @see Type#INSERT
   * @see Type#DELETE
   * @see Type#UPDATE
   */
  public DataStoreEvent(Object source, Map<Type, DBIDs> objects) {
    super(source);
    this.objects = objects;
  }

  /**
   * Returns the types of change this event consists of.
   * 
   * @see Type#INSERT
   * @see Type#DELETE
   * @see Type#UPDATE
   * 
   * @return the types of this event
   */
  public Set<Type> getTypes() {
    return objects.keySet();
  }

  /**
   * Returns the objects that have been changed and the type of change.
   * 
   * @return the objects that have been changed
   */
  public Map<Type, DBIDs> getObjects() {
    return objects;
  }
}