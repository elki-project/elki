package de.lmu.ifi.dbs.elki.database.datastore;

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
   * Associates the specified value with the specified id in this storage. If
   * the storage previously contained a value for the id, the previous value is
   * replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  public T put(DBID id, T value);

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
