package experimentalcode.erich.newdblayer.storage.memory;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.storage.StorageIDMap;
import experimentalcode.erich.newdblayer.storage.WritableStorage;

/**
 * A class to answer representation queries using the stored Array.
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
public class ArrayStorage<T> implements WritableStorage<T> {
  /**
   * Data array
   */
  private Object[] data;
  
  /**
   * DBID to index map
   */
  private StorageIDMap idmap;

  /**
   * Constructor.
   */
  public ArrayStorage(Object[] data, StorageIDMap idmap) {
    super();
    this.data = data;
    this.idmap = idmap;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(DBID id) {
    try {
      return (T) data[idmap.map(id)];
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

  @Override
  public T set(DBID id, T value) {
    T ret = get(id);
    data[idmap.map(id)] = value;
    return ret;
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
  }
}