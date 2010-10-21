package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.PrimitiveResult;

/**
 * Generic storage interface for objects indexed by {@link DBID}.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data type
 */
public interface DataStore<T> extends PrimitiveResult {
  /**
   * Retrieves an object from the storage.
   * 
   * @param id Database ID.
   * @return Object or {@code null}
   */
  public T get(DBID id);

  /**
   * Adds a listener for the <code>DataStoreEvent</code> posted after the
   * content of the storage changes.
   * 
   * @param l the listener to add
   * @see #removeDataStoreListener
   */
  void addDataStoreListener(DataStoreListener<T> l);

  /**
   * Removes a listener previously added with <code>addDataStoreListener</code>.
   * 
   * @param l the listener to remove
   * @see #addDataStoreListener
   */
  void removeDataStoreListener(DataStoreListener<T> l);
}
