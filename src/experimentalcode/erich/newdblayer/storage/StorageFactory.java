package experimentalcode.erich.newdblayer.storage;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import experimentalcode.erich.newdblayer.storage.memory.MemoryStorageFactory;

/**
 * API for a storage factory used for producing larger storage maps.
 * 
 * Use {@link #FACTORY} for a static instance.
 * 
 * TODO: use this in all algorithms
 * 
 * @author Erich Schubert
 */
public interface StorageFactory {
  /**
   * Static storage factory
   */
  public static StorageFactory FACTORY = new MemoryStorageFactory();
  
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
   * Make a new storage, to associate the given ids with an object of class dataclass.
   * 
   * @param <T> stored data type
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclass class to store
   * @return
   */
  public <T> WritableStorage<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass);
  
  /**
   * Make a new record storage, to associate the given ids with an object of class dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclasses classes to store
   * @return
   */
  public WritableRecordStorage makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses);
}