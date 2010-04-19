package experimentalcode.erich.newdblayer.storage;

import experimentalcode.erich.newdblayer.ids.DBID;

/**
 * Writable data store.
 * 
 * @author Erich Schubert
 *
 * @param <T>
 */
public interface WritableStorage<T> extends Storage<T> {
  /**
   * Retrieve an object from the storage.
   * 
   * @param id Database ID.
   * @param value Value to store.
   * @return previous value
   */
  public T set(DBID id, T value);

  /**
   * Deallocate the storage, freeing the memory.
   */
  public void destroy();
}
