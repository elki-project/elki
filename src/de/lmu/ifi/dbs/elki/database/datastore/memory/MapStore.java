package de.lmu.ifi.dbs.elki.database.datastore.memory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import de.lmu.ifi.dbs.elki.database.datastore.AbstractDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

/**
 * A class to answer representation queries using a map. Basically, it is just a
 * wrapper around a regular map.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Representation object type
 */
public class MapStore<T> extends AbstractDataStore<T> implements WritableDataStore<T> {
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
    this.data = new ConcurrentHashMap<DBID, T>();
  }

  @Override
  public T get(DBID id) {
    return data.get(id);
  }

  @Override
  public T put(DBID id, T value) {
    // deletion
    if(value == null) {
      T old = data.remove(id);
      fireContentChanged(null, null, DBIDUtil.newArray(id));
      return old;
    }

    T old = data.put(id, value);

    if(old == null) {
      // insertion
      fireContentChanged(null, DBIDUtil.newArray(id), null);
    }
    else {
      // update
      fireContentChanged(DBIDUtil.newArray(id), null, null);
    }
    return old;
  }

  @Override
  public void putAll(Map<DBID, T> map) {
    ArrayModifiableDBIDs insertions = DBIDUtil.newArray();
    ArrayModifiableDBIDs updates = DBIDUtil.newArray();

    for(Entry<DBID, T> entry : map.entrySet()) {
      DBID id = entry.getKey();
      T value = entry.getValue();
      T old = data.put(id, value);
      if(old == null) {
        insertions.add(id);
      }
      else {
        updates.add(id);
      }
    }

    fireContentChanged(updates, insertions, null);
  }

  @Override
  public void destroy() {
    data = null;
    fireDataStoreDestroyed();
  }

  @Override
  public void delete(DBID id) {
    data.remove(id);
    fireContentChanged(null, null, DBIDUtil.newArray(id));
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