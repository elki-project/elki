package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Storage utility class. Mostly a shorthand for {@link DataStoreFactory#FACTORY}.
 * 
 * @author Erich Schubert
 */
public final class DataStoreUtil {
  /**
   * Make a new storage, to associate the given ids with an object of class dataclass.
   * 
   * @param <T> stored data type
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclass class to store
   * @return new data store
   */
  public static <T> WritableDataStore<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass) {
    return DataStoreFactory.FACTORY.makeStorage(ids, hints, dataclass);
  }
  
  /**
   * Make a new record storage, to associate the given ids with an object of class dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclasses classes to store
   * @return new record store
   */
  public static WritableRecordStore makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses) {
    return DataStoreFactory.FACTORY.makeRecordStorage(ids, hints, dataclasses);
  }
}
