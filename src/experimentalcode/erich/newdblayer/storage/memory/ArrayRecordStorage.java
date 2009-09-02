package experimentalcode.erich.newdblayer.storage.memory;

import experimentalcode.erich.newdblayer.DBID;
import experimentalcode.erich.newdblayer.storage.DBIDMap;
import experimentalcode.erich.newdblayer.storage.Storage;

/**
 * A class to answer representation queries using the stored Array.
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
public class ArrayRecordStorage<T> implements Storage<T> {
  /**
   * Representation index.
   */
  private final int index;
  
  /**
   * Data array
   */
  private final Object[][] data;
  
  /**
   * DBID to index map
   */
  private final DBIDMap idmap;

  /**
   * Constructor.
   * 
   * @param index Representation index.
   */
  public ArrayRecordStorage(int index, Object[][] data, DBIDMap idmap) {
    super();
    this.index = index;
    this.data = data;
    this.idmap = idmap;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(DBID id) {
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
}