package experimentalcode.erich.newdblayer.storage.memory;

import experimentalcode.erich.newdblayer.ids.DBIDRangeAllocation;
import experimentalcode.erich.newdblayer.ids.DBIDs;
import experimentalcode.erich.newdblayer.storage.RangeIDMap;
import experimentalcode.erich.newdblayer.storage.StorageFactory;
import experimentalcode.erich.newdblayer.storage.WritableRecordStorage;
import experimentalcode.erich.newdblayer.storage.WritableStorage;

/**
 * Simple factory class that will store all data in memory using object arrays or hashmaps.
 * 
 * @author Erich Schubert
 */
public class MemoryStorageFactory implements StorageFactory {
  @Override
  public <T> WritableStorage<T> makeStorage(DBIDs ids, @SuppressWarnings("unused") Class<? super T> dataclass) {
    if (ids instanceof DBIDRangeAllocation) {
      DBIDRangeAllocation range = (DBIDRangeAllocation) ids;
      Object[] data = new Object[range.len];
      return new ArrayStorage<T>(data, new RangeIDMap(range));
    } else {
      return new MapStorage<T>();
    }
  }

  @Override
  public WritableRecordStorage makeRecordStorage(DBIDs ids, Class<?>... dataclasses) {
    if (ids instanceof DBIDRangeAllocation) {
      DBIDRangeAllocation range = (DBIDRangeAllocation) ids;
      Object[][] data = new Object[range.len][dataclasses.length];
      return new ArrayRecordStorage(data, new RangeIDMap(range));
    } else {
      return new MapRecordStorage(dataclasses.length);
    }
  }
}
