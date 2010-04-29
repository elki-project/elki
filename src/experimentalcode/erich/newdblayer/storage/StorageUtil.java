package experimentalcode.erich.newdblayer.storage;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Storage utility class. Mostly a shorthand for {@link StorageFactory#FACTORY}.
 * 
 * @author Erich Schubert
 */
public final class StorageUtil {
  /**
   * Make a new storage, to associate the given ids with an object of class dataclass.
   * 
   * @param <T> stored data type
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclass class to store
   * @return
   */
  public static <T> WritableStorage<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass) {
    return StorageFactory.FACTORY.makeStorage(ids, hints, dataclass);
  }
  
  /**
   * Make a new record storage, to associate the given ids with an object of class dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclasses classes to store
   * @return
   */
  public static WritableRecordStorage makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses) {
    return StorageFactory.FACTORY.makeRecordStorage(ids, hints, dataclasses);
  }
}
