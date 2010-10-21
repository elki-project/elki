package de.lmu.ifi.dbs.elki.database.datastore.memory;

import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.database.datastore.AbstractDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

/**
 * A class to answer representation queries using the stored Array.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Representation object type
 */
public class ArrayStore<T> extends AbstractDataStore<T> implements WritableDataStore<T> {
  /**
   * Data array
   */
  private Object[] data;

  /**
   * DBID to index map
   */
  private DataStoreIDMap idmap;

  /**
   * Constructor.
   */
  public ArrayStore(Object[] data, DataStoreIDMap idmap) {
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
  public T put(DBID id, T value) {
    T old = get(id);
    data[idmap.map(id)] = value;

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
      T old = get(id);
      data[idmap.map(id)] = value;
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
    idmap = null;
    fireDataStoreDestroyed();
  }

  @Override
  public void delete(@SuppressWarnings("unused") DBID id) {
    throw new UnsupportedOperationException("Can't delete from a static array storage.");
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