package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Generic storage interface for objects indexed by {@link DBID}.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type
 */
public interface DataStore<T> {
  /**
   * Retrieve an object from the storage.
   * 
   * @param id Database ID.
   * @return Object or {@code null}
   */
  public T get(DBID id);
}
