package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.datastore.memory.MemoryDataStoreFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * API for a storage factory used for producing larger storage maps.
 * 
 * Use {@link #FACTORY} for a static instance.
 * 
 * TODO: use this in all algorithms
 * 
 * @author Erich Schubert
 */
public interface DataStoreFactory {
  /**
   * Static storage factory
   */
  public static DataStoreFactory FACTORY = new MemoryDataStoreFactory();
  
  /**
   * Storage will be used only temporary.
   */
  public final static int HINT_TEMP = 0x01;
  
  /**
   * "Hot" data, that will be used a lot, preferring memory storage.
   */
  public final static int HINT_HOT = 0x02;
  
  /**
   * "static" data, that will not change often
   */
  public final static int HINT_STATIC = 0x04;
  
  /**
   * Data that might require sorted access (so hashmaps are suboptimal)
   */
  public final static int HINT_SORTED = 0x08;
  
  /**
   * Data that is the main database. Includes HOT, STATIC, SORTED
   */
  public final static int HINT_DB = 0x1E;
  
  /**
   * Make a new storage, to associate the given ids with an object of class dataclass.
   * 
   * @param <T> stored data type
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclass class to store
   * @return
   */
  public <T> WritableDataStore<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass);
  
  /**
   * Make a new record storage, to associate the given ids with an object of class dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclasses classes to store
   * @return
   */
  public WritableRecordStore makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses);
}