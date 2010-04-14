package experimentalcode.erich.newdblayer.storage.memory;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.storage.StorageIDMap;
import experimentalcode.erich.newdblayer.storage.WritableRecordStorage;
import experimentalcode.erich.newdblayer.storage.WritableStorage;

/**
 * A class to answer representation queries using the stored Array.
 * 
 * @author Erich Schubert
 */
public class ArrayRecordStorage implements WritableRecordStorage {
  /**
   * Data array
   */
  private final Object[][] data;

  /**
   * DBID to index map
   */
  private final StorageIDMap idmap;

  /**
   * Constructor with existing data
   * 
   * @param data Existing data
   * @param idmap Map for array offsets
   */
  public ArrayRecordStorage(Object[][] data, StorageIDMap idmap) {
    super();
    this.data = data;
    this.idmap = idmap;
  }

  @Override
  public <T> WritableStorage<T> getStorage(int col, @SuppressWarnings("unused") Class<? super T> datatype) {
    // TODO: add type checking safety?
    return new StorageAccessor<T>(col);
  }
  
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
  
  protected <T> void set(DBID id, int index, T value) {
    data[idmap.map(id)][index] = value;
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
      return ArrayRecordStorage.this.get(id, index);
    }

    @Override
    public void set(DBID id, T value) {
      ArrayRecordStorage.this.set(id, index, value);
    }
  }
}