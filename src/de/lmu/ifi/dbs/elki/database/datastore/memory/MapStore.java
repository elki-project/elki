package de.lmu.ifi.dbs.elki.database.datastore.memory;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;


/**
 * A class to answer representation queries using a map.
 * Basically, it is just a wrapper around a regular map.
 * 
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
public class MapStore<T> implements WritableDataStore<T> {
  /**
   * Storage Map
   */
  private Map<DBID, T> data;

  /**
   * Constructor.
   * 
   * @param data Existing map
   */
  public MapStore(Map<DBID, T> data) {
    super();
    this.data = data;
  }

  /**
   * Constructor.
   */
  public MapStore() {
    super();
    this.data = new HashMap<DBID, T>();
  }

  @Override
  public T get(DBID id) {
    return data.get(id);
  }

  @Override
  public T put(DBID id, T value) {
    return data.put(id, value);
  }

  @Override
  public void destroy() {
    data = null;
  }

  @Override
  public void delete(DBID id) {
    data.remove(id);
  }

  @Override
  public String getName() {
    return "raw";
  }
}