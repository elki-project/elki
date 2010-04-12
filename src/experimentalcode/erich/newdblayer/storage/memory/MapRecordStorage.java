package experimentalcode.erich.newdblayer.storage.memory;

import java.util.Map;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.storage.Storage;

/**
 * A class to answer representation queries using a map and an index within the record.
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
// TODO: currently unused, so far we can use ArrayRecordStorage which is more efficient.
public class MapRecordStorage<T> implements Storage<T> {
  /**
   * Representation index.
   */
  private final int index;
  
  /**
   * Storage Map
   */
  private final Map<DBID, Object[]> data;

  /**
   * Constructor.
   * 
   * @param index Representation index.
   */
  public MapRecordStorage(int index, Map<DBID, Object[]> data) {
    super();
    this.index = index;
    this.data = data;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(DBID id) {
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
  }
}