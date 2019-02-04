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
package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;

/**
 * Class to manage database events such as insertions and removals.
 *
 * @author Elke Achtert
 * @since 0.4.0
 * @has - - - ResultListener
 */
public class DatabaseEventManager {
  /**
   * Holds the listeners for data store changes
   */
  private List<DataStoreListener> dataListenerList = new ArrayList<>();

  /**
   * Holds the listeners for result changes.
   */
  private List<ResultListener> resultListenerList = new ArrayList<>();

  /**
   * Indicates whether DataStoreEvents should be accumulated and fired as one
   * event on demand.
   */
  private boolean accumulateDataStoreEvents = false;

  /**
   * The type of the current DataStoreEvent to be accumulated.
   */
  private Type currentDataStoreEventType = null;

  /**
   * Types for aggregation.
   */
  private enum Type {
    INSERT, REMOVE, UPDATE
  };

  /**
   * The objects that were changed in the current DataStoreEvent.
   */
  private HashSetModifiableDBIDs dataStoreObjects;

  /**
   * Collects successive insertion, deletion or update events. The accumulated
   * event will be fired when {@link #flushDataStoreEvents()} is called or a
   * different event type occurs.
   *
   * @see #flushDataStoreEvents()
   * @see DataStoreEvent
   */
  public void accumulateDataStoreEvents() {
    this.accumulateDataStoreEvents = true;
  }

  /**
   * Fires all collected insertion, deletion or update events as one
   * DataStoreEvent, i.e. notifies all registered DataStoreListener how the
   * content of the database has been changed since
   * {@link #accumulateDataStoreEvents()} was called.
   *
   * @see #accumulateDataStoreEvents
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  public void flushDataStoreEvents() {
    DataStoreEvent e;
    switch(currentDataStoreEventType){
    case INSERT:
      e = DataStoreEvent.insertionEvent(dataStoreObjects);
      break;
    case REMOVE:
      e = DataStoreEvent.removalEvent(dataStoreObjects);
      break;
    case UPDATE:
      e = DataStoreEvent.updateEvent(dataStoreObjects);
      break;
    default:
      return;
    }

    for(int i = dataListenerList.size(); --i >= 0;) {
      dataListenerList.get(i).contentChanged(e);
    }
    // reset
    accumulateDataStoreEvents = false;
    currentDataStoreEventType = null;
    dataStoreObjects = null;
  }

  /**
   * Adds a <code>DataStoreListener</code> for a <code>DataStoreEvent</code>
   * posted after the content of the database changes.
   *
   * @param l the listener to add
   * @see #removeListener(DataStoreListener)
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  public void addListener(DataStoreListener l) {
    dataListenerList.add(l);
  }

  /**
   * Removes a <code>DataStoreListener</code> previously added with
   * {@link #addListener(DataStoreListener)}.
   *
   * @param l the listener to remove
   * @see #addListener(DataStoreListener)
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  public void removeListener(DataStoreListener l) {
    dataListenerList.remove(l);
  }

  /**
   * Adds a <code>ResultListener</code> to be notified on new results.
   *
   * @param l the listener to add
   * @see #removeListener(ResultListener)
   * @see ResultListener
   * @see Result
   */
  public void addListener(ResultListener l) {
    resultListenerList.add(l);
  }

  /**
   * Removes a <code>ResultListener</code> previously added with
   * {@link #addListener(ResultListener)}.
   *
   * @param l the listener to remove
   * @see #addListener(ResultListener)
   * @see ResultListener
   * @see Result
   *
   */
  public void removeListener(ResultListener l) {
    resultListenerList.remove(l);
  }

  /**
   * Event when new objects are inserted.
   *
   * @param insertions the objects that have been inserted
   */
  public void fireObjectsInserted(DBIDs insertions) {
    fireObjectsChanged(insertions, Type.INSERT);
  }

  /**
   * Event when a new object was inserted.
   *
   * @param insertion the object that was inserted
   */
  public void fireObjectInserted(DBIDRef insertion) {
    fireObjectChanged(insertion, Type.INSERT);
  }

  /**
   * Event when objects have changed / were updated.
   *
   * @param updates the objects that have been updated
   */
  public void fireObjectsUpdated(DBIDs updates) {
    fireObjectsChanged(updates, Type.UPDATE);
  }

  /**
   * Event when an object was changed / updated.
   *
   * @param update the object that was updated
   */
  public void fireObjectsUpdated(DBIDRef update) {
    fireObjectChanged(update, Type.UPDATE);
  }

  /**
   * Event when objects were removed / deleted.
   *
   * @param deletions the objects that have been removed
   */
  protected void fireObjectsRemoved(DBIDs deletions) {
    fireObjectsChanged(deletions, Type.REMOVE);
  }

  /**
   * Event when an objects was removed / deleted.
   *
   * @param deletion the object that has was removed
   */
  protected void fireObjectRemoved(DBIDRef deletion) {
    fireObjectChanged(deletion, Type.REMOVE);
  }

  /**
   * Handles a DataStoreEvent with the specified type. If the current event type
   * is not equal to the specified type, the events accumulated up to now will
   * be fired first.
   *
   * The new event will be aggregated and fired on demand if
   * {@link #accumulateDataStoreEvents} is set, otherwise all registered
   * <code>DataStoreListener</code> will be notified immediately that the
   * content of the database has been changed.
   *
   * @param objects the objects that have been changed, i.e. inserted, deleted
   *        or updated
   */
  private void fireObjectsChanged(DBIDs objects, Type type) {
    // flush first
    if(currentDataStoreEventType != null && !currentDataStoreEventType.equals(type)) {
      flushDataStoreEvents();
    }
    if(accumulateDataStoreEvents) {
      if(this.dataStoreObjects == null) {
        this.dataStoreObjects = DBIDUtil.newHashSet();
      }
      this.dataStoreObjects.addDBIDs(objects);
      currentDataStoreEventType = type;
      return;
    }
    // Execute immediately:
    DataStoreEvent e;
    switch(type){
    case INSERT:
      e = DataStoreEvent.insertionEvent(objects);
      break;
    case REMOVE:
      e = DataStoreEvent.removalEvent(objects);
      break;
    case UPDATE:
      e = DataStoreEvent.updateEvent(objects);
      break;
    default:
      return;
    }

    for(int i = dataListenerList.size(); --i >= 0;) {
      dataListenerList.get(i).contentChanged(e);
    }
  }

  /**
   * Handles a DataStoreEvent with the specified type. If the current event type
   * is not equal to the specified type, the events accumulated up to now will
   * be fired first.
   *
   * The new event will be aggregated and fired on demand if
   * {@link #accumulateDataStoreEvents} is set, otherwise all registered
   * <code>DataStoreListener</code> will be notified immediately that the
   * content of the database has been changed.
   *
   * @param object the object that has been changed, i.e. inserted, deleted or
   *        updated
   */
  private void fireObjectChanged(DBIDRef object, Type type) {
    // flush first
    if(currentDataStoreEventType != null && !currentDataStoreEventType.equals(type)) {
      flushDataStoreEvents();
    }
    if(this.dataStoreObjects == null) {
      this.dataStoreObjects = DBIDUtil.newHashSet();
    }
    this.dataStoreObjects.add(object);
    currentDataStoreEventType = type;

    if(!accumulateDataStoreEvents) {
      flushDataStoreEvents();
    }
  }

  /**
   * Informs all registered <code>ResultListener</code> that a new result was
   * added.
   *
   * @param r New child result added
   * @param parent Parent result that was added to
   */
  public void fireResultAdded(Result r, Result parent) {
    for(int i = resultListenerList.size(); --i >= 0;) {
      resultListenerList.get(i).resultAdded(r, parent);
    }
  }

  /**
   * Informs all registered <code>ResultListener</code> that a new result has
   * been removed.
   *
   * @param r result that has been removed
   * @param parent Parent result that has been removed
   */
  public void fireResultRemoved(Result r, Result parent) {
    for(int i = resultListenerList.size(); --i >= 0;) {
      resultListenerList.get(i).resultRemoved(r, parent);
    }
  }
}
