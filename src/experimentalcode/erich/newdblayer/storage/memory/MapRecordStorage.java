package experimentalcode.erich.newdblayer.storage.memory;

import java.util.HashMap;
import java.util.Map;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.storage.WritableRecordStorage;
import experimentalcode.erich.newdblayer.storage.WritableStorage;

/**
 * A class to answer representation queries using a map and an index within the record.
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
public class MapRecordStorage implements WritableRecordStorage {
  /**
   * Record length
   */
  private final int rlen;
  
  /**
   * Storage Map
   */
  private final Map<DBID, Object[]> data;

  /**
   * Constructor with existing data.
   * 
   * @param rlen Number of columns (record length)
   * @param data Existing data map
   */
  public MapRecordStorage(int rlen, Map<DBID, Object[]> data) {
    super();
    this.rlen = rlen;
    this.data = data;
  }

  /**
   * Constructor without existing data.
   * 
   * @param rlen Number of columns (record length)
   */
  public MapRecordStorage(int rlen) {
    this(rlen, new HashMap<DBID, Object[]>());
  }

  @Override
  public <T> WritableStorage<T> getStorage(int col, @SuppressWarnings("unused") Class<? super T> datatype) {
    // TODO: add type checking?
    return new StorageAccessor<T>(col);
  }

  /**
   * Actual getter
   * 
   * @param id Database ID
   * @param index column index
   * @return current value
   */
  @SuppressWarnings("unchecked")
  protected <T> T get(DBID id, int index) {
    Object[] d = data.get(id);
    if (d == null) {
      return null;
    }
    try {
      return (T) d[index];
    }
    catch(ClassCastException e) {
      return null;
    }
    catch(ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  /**
   * Actual setter
   * 
   * @param id Database ID
   * @param index column index
   * @param value new value
   * @return previous value
   */
  @SuppressWarnings("unchecked")
  protected <T> T set(DBID id, int index, T value) {
    Object[] d = data.get(id);
    if (d == null) {
      d = new Object[rlen];
      data.put(id, d);
    }
    T ret = (T) d[index];
    d[index] = value;
    return ret;
  }

  /**
   * Access a single record in the given data.
   * 
   * @author Erich Schubert
   *
   * @param <T> Object data type to access 
   */
  protected class StorageAccessor<T> implements WritableStorage<T> {
    /**
     * Representation index.
     */
    private final int index;

    /**
     * Constructor.
     * 
     * @param index In-record index
     */
    protected StorageAccessor(int index) {
      super();
      this.index = index;
    }

    @Override
    public T get(DBID id) {
      return MapRecordStorage.this.get(id, index);
    }

    @Override
    public T set(DBID id, T value) {
      return MapRecordStorage.this.set(id, index, value);
    }

    @Override
    public void destroy() {
      throw new UnsupportedOperationException("Record storage accessors cannot be destroyed.");
    }
  }
}