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
   * Retrieve an object from the storage.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  public T put(DBID id, T value);

  /**
   * Deallocate the storage, freeing the memory.
   */
  public void destroy();

  /**
   * Delete the contents for a particular ID.
   * 
   * @param id Database ID.
   */
  public void delete(DBID id);
}
