package experimentalcode.erich.newdblayer.storage;

import experimentalcode.erich.newdblayer.ids.DBIDs;
import experimentalcode.erich.newdblayer.storage.memory.MemoryStorageFactory;

/**
 * API for a storage factory used for producing larger storage maps.
 * 
 * Use {@link #FACTORY} for a static instance.
 * 
 * TODO: extend this to allow record storages as used in DBConnection?
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
   * Make a new storage, to associate the given ids with an object of class dataclass.
   * 
   * @param <T> stored data type
   * @param ids DBIDs to store data for
   * @param dataclass class to store
   * @return
   */
  public <T> WritableStorage<T> makeStorage(DBIDs ids, Class<? super T> dataclass);
  
  /**
   * Make a new record storage, to associate the given ids with an object of class dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param dataclasses classes to store
   * @return
   */
  public WritableRecordStorage makeRecordStorage(DBIDs ids, Class<?>... dataclasses);
}