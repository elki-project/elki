package de.lmu.ifi.dbs.elki.database.datastore;

/**
 * Represents a storage which stores multiple values per object in a record fashion.
 * 
 * @author Erich Schubert
 */
public interface RecordStore {
  /**
   * Get a {@link DataStore} instance for a particular record column.
   * 
   * @param <T> Data type
   * @param col Column number
   * @param datatype data class
   * @return writable storage
   */
  public <T> DataStore<T> getStorage(int col, Class<? super T> datatype);
}
