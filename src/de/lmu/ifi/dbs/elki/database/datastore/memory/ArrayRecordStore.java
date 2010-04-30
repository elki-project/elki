package de.lmu.ifi.dbs.elki.database.datastore.memory;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * A class to answer representation queries using the stored Array.
 * 
 * @author Erich Schubert
 */
public class ArrayRecordStore implements WritableRecordStore {
  /**
   * Data array
   */
  private final Object[][] data;

  /**
   * DBID to index map
   */
  private final DataStoreIDMap idmap;

  /**
   * Constructor with existing data
   * 
   * @param data Existing data
   * @param idmap Map for array offsets
   */
  public ArrayRecordStore(Object[][] data, DataStoreIDMap idmap) {
    super();
    this.data = data;
    this.idmap = idmap;
  }

  @Override
  public <T> WritableDataStore<T> getStorage(int col, @SuppressWarnings("unused") Class<? super T> datatype) {
    // TODO: add type checking safety?
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
    try {
      return (T) data[idmap.map(id)][index];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      return null;
    }
    catch(NullPointerException e) {
      return null;
    }
    catch(ClassCastException e) {
      return null;
    }
  }
  
  /**
   * Actual setter
   * 
   * @param id Database ID
   * @param index column index
   * @param value New value
   * @return old value
   */
  @SuppressWarnings("unchecked")
  protected <T> T set(DBID id, int index, T value) {
    T ret = (T) data[idmap.map(id)][index];
    data[idmap.map(id)][index] = value;
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

    @Override
    public T get(DBID id) {
      return ArrayRecordStore.this.get(id, index);
    }

    @Override
    public T put(DBID id, T value) {
      return ArrayRecordStore.this.set(id, index, value);
    }

    @Override
    public void destroy() {
      throw new UnsupportedOperationException("ArrayStore record columns cannot be destroyed.");
    }

    @Override
    public void delete(@SuppressWarnings("unused") DBID id) {
      throw new UnsupportedOperationException("ArrayStore record values cannot be deleted.");
    }
  }

  @Override
  public boolean remove(@SuppressWarnings("unused") DBID id) {
    throw new UnsupportedOperationException("ArrayStore records cannot be removed.");
  }
}