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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.EventObject;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Encapsulates information describing changes of the k nearest neighbors (kNNs)
 * of some objects due to insertion or removal of objects. Used to notify all
 * subscribed {@link KNNListener} of the change.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 * 
 * @has - - - DBIDs
 * 
 * @see KNNListener
 */
public class KNNChangeEvent extends EventObject {
  /**
   * Serialization ID since Java EventObjects are expected to be serializable.
   */
  private static final long serialVersionUID = 513913140334355886L;

  /**
   * Available event types.
   */
  public enum Type {
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
   * Holds the type of this change event.
   * 
   * @see Type
   */
  private final Type type;

  /**
   * The ids of the kNNs that were inserted or deleted due to the insertion or
   * removals of objects.
   */
  private final DBIDs objects;

  /**
   * The ids of the kNNs that were updated due to the insertion or removals of
   * objects.
   */
  private final DBIDs updates;

  /**
   * Used to create an event when kNNs of some objects have been changed.
   * 
   * @param source the object responsible for generating the event
   * @param type the type of change
   * @param objects the ids of the removed or inserted kNNs (according to the
   *        type of this event)
   * @param updates the ids of kNNs which have been changed due to the removals
   *        or insertions
   * @see Type#INSERT
   * @see Type#DELETE
   */
  public KNNChangeEvent(Object source, Type type, DBIDs objects, DBIDs updates) {
    super(source);
    this.type = type;
    this.objects = objects;
    this.updates = updates;
  }

  /**
   * Returns the type of this change event.
   * 
   * @return {@link Type#INSERT} or {@link Type#DELETE}
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the ids of the removed or inserted kNNs (according to the type of
   * this event).
   * 
   * @return the ids of the removed or inserted kNNs
   */
  public DBIDs getObjects() {
    return objects;
  }

  /**
   * Returns the ids of kNNs which have been changed due to the removals or
   * insertions.
   * 
   * @return the ids of kNNs which have been changed
   */
  public DBIDs getUpdates() {
    return updates;
  }
}