package experimentalcode.erich.newdblayer.storage;

/**
 * Represents a storage which stores multiple values per object in a record fashion.
 * 
 * @author Erich Schubert
 */
public interface RecordStorage {
  /**
   * Get a {@link Storage} instance for a particular record column.
   * 
   * @param <T> Data type
   * @param col Column number
   * @param datatype data class
   * @return writable storage
   */
  public <T> Storage<T> getStorage(int col, Class<? super T> datatype);
}
