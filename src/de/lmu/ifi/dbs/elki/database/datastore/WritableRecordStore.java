package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Represents a storage which stores multiple values per object in a record fashion.
 * 
 * @author Erich Schubert
 */
public interface WritableRecordStore extends RecordStore {
  /**
   * Get a {@link WritableDataStore} instance for a particular record column.
   * 
   * @param <T> Data type
   * @param col Column number
   * @param datatype data class
   * @return writable storage
   */
  @Override
  public <T> WritableDataStore<T> getStorage(int col, Class<? super T> datatype);
  
  /**
   * Remove an object from the store, all columns.
   * 
   * @param id object ID to remove
   * @return success code
   */
  public boolean remove(DBID id);
}
