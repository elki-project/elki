package experimentalcode.erich.newdblayer.storage.memory;

import java.util.HashMap;
import java.util.Map;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.storage.WritableStorage;

/**
 * A class to answer representation queries using a map.
 * Basically, it is just a wrapper around a regular map.
 * 
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
public class MapStorage<T> implements WritableStorage<T> {
  /**
   * Storage Map
   */
  private final Map<DBID, T> data;

  /**
   * Constructor.
   * 
   * @param data Existing map
   */
  public MapStorage(Map<DBID, T> data) {
    super();
    this.data = data;
  }

  /**
   * Constructor.
   */
  public MapStorage() {
    super();
    this.data = new HashMap<DBID, T>();
  }

  @Override
  public T get(DBID id) {
    return data.get(id);
  }

  @Override
  public void set(DBID id, T value) {
    data.put(id, value);
  }
}