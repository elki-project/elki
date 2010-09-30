package de.lmu.ifi.dbs.elki.database.datastore.memory;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * A class to answer representation queries using the stored Array.
 * @author Erich Schubert
 *
 * @param <T> Representation object type
 */
public class ArrayStore<T> implements WritableDataStore<T> {
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
    T ret = get(id);
    data[idmap.map(id)] = value;
    return ret;
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
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