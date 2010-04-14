package experimentalcode.erich.newdblayer.storage;

/**
 * Represents a storage which stores multiple values per object in a record fashion.
 * 
 * @author Erich Schubert
 */
public interface WritableRecordStorage extends RecordStorage {
  /**
   * Get a {@link WritableStorage} instance for a particular record column.
   * 
   * @param <T> Data type
   * @param col Column number
   * @param datatype data class
   * @return writable storage
   */
  @Override
  public <T> WritableStorage<T> getStorage(int col, Class<? super T> datatype);
}
