package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Generic storage interface for objects indexed by {@link DBID}.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type
 */
// TODO: remove "Result" and getName() methods?
public interface DataStore<T> extends Result {
  /**
   * Retrieve an object from the storage.
   * 
   * @param id Database ID.
   * @return Object or {@code null}
   */
  public T get(DBID id);
}
