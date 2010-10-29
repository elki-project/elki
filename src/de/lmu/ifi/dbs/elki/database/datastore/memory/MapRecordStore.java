package de.lmu.ifi.dbs.elki.database.datastore.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;


/**
 * A class to answer representation queries using a map and an index within the record.
 * @author Erich Schubert
 */
public class MapRecordStore implements WritableRecordStore {
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
  public MapRecordStore(int rlen, Map<DBID, Object[]> data) {
    super();
    this.rlen = rlen;
    this.data = data;
  }

  /**
   * Constructor without existing data.
   * 
   * @param rlen Number of columns (record length)
   */
  public MapRecordStore(int rlen) {
    this(rlen, new ConcurrentHashMap<DBID, Object[]>());
  }

  @Override
  public <T> WritableDataStore<T> getStorage(int col, @SuppressWarnings("unused") Class<? super T> datatype) {
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
  protected class StorageAccessor<T> implements WritableDataStore<T> {
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

    @SuppressWarnings("unchecked")
    @Override
    public T get(DBID id) {
      return (T) MapRecordStore.this.get(id, index);
    }

    @Override
    public T put(DBID id, T value) {
      return MapRecordStore.this.set(id, index, value);
    }

    @Override
    public void destroy() {
      throw new UnsupportedOperationException("Record storage accessors cannot be destroyed.");
    }

    @Override
    public void delete(@SuppressWarnings("unused") DBID id) {
      throw new UnsupportedOperationException("Record storage values cannot be deleted.");
    }

    @Override
    public String getLongName() {
      return "raw";
    }

    @Override
    public String getShortName() {
      return "raw";
    }
  }

  @Override
  public boolean remove(DBID id) {
    return data.remove(id) != null;
  }

}