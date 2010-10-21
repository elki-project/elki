package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Writable data store.
 * 
 * @author Erich Schubert
 * 
 * @param <T>
 */
public interface WritableDataStore<T> extends DataStore<T> {
  /**
   * Associates the specified value with the specified id in this storage and
   * notifies the registered listeners. If the storage previously contained a
   * value for the id, the previous value is replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  public T put(DBID id, T value);

  /**
   * Stores all of the id-value mappings from the specified map in this storage
   * and notifies the registered listeners.
   * If the storage previously contained a value for an id, the previous value
   * is replaced. 
   * 
   * After storing all id-value pairs one insertion and/or changed event for the
   * whole operation will be fired. In contrast to {@code put(id, value)} which
   * informs the listeners after each insertion or change, the registered
   * listeners will be notified in one bulk.
   * 
   * @param map the id-value mappings to be stored
   */
  public void putAll(Map<DBID, T> map);

  /**
   * Deallocate the storage, freeing the memory and notifies the registered
   * listeners.
   */
  public void destroy();

  /**
   * Delete the contents for a particular ID and notifies the registered
   * listeners.
   * 
   * @param id Database ID.
   */
  public void delete(DBID id);
}
