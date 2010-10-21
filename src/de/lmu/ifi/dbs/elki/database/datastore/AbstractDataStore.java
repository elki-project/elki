package de.lmu.ifi.dbs.elki.database.datastore;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Encapsulates the methods necessary for listener notifications.
 * 
 * @author Elke Achtert
 * 
 * @param <T> the data type
 */
public abstract class AbstractDataStore<T> implements DataStore<T> {

  /**
   * Holds the listener of this datastore.
   */
  private EventListenerList listenerList = new EventListenerList();

  @Override
  public void addDataStoreListener(DataStoreListener<T> l) {
    listenerList.add(DataStoreListener.class, l);
  }

  @Override
  public void removeDataStoreListener(DataStoreListener<T> l) {
    listenerList.remove(DataStoreListener.class, l);
  }

  /**
   * Notifies all listeners that the content of this storage has been changed.
   * 
   * @param updates the ids of the objects that have been updated
   * @param insertions the ids of the objects that have been newly inserted
   * @param deletions the ids of the objects that have been removed
   */
  @SuppressWarnings("unchecked")
  protected void fireContentChanged(DBIDs updates, DBIDs insertions, DBIDs deletions) {
    Object[] listeners = listenerList.getListenerList();
    DataStoreEvent<T> e = new DataStoreEvent<T>(this, updates, insertions, deletions);

    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == DataStoreListener.class) {
        ((DataStoreListener<T>) listeners[i + 1]).contentChanged(e);
      }
    }
  }

  /**
   * Notifies all listeners that the datastore has been destroyed.
   */
  @SuppressWarnings("unchecked")
  protected void fireDataStoreDestroyed() {
    Object[] listeners = listenerList.getListenerList();
    DataStoreEvent<T> e = new DataStoreEvent<T>(this, null, null, null);

    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == DataStoreListener.class) {
        ((DataStoreListener<T>) listeners[i + 1]).dataStoreDestroyed(e);
      }
    }
  }
}
