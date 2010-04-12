package experimentalcode.erich.newdblayer.storage;

import experimentalcode.erich.newdblayer.ids.DBID;

/**
 * Generic storage interface for objects indexed by {@link DBID}.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type
 */
public interface Storage<T> {
  /**
   * Retrieve an object from the storage.
   * 
   * @param id Database ID.
   * @return Object or {@code null}
   */
  public T get(DBID id);
}
