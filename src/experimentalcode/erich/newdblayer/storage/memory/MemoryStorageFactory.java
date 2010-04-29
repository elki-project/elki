package experimentalcode.erich.newdblayer.storage.memory;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.RangeDBIDs;
import experimentalcode.erich.newdblayer.storage.RangeIDMap;
import experimentalcode.erich.newdblayer.storage.StorageFactory;
import experimentalcode.erich.newdblayer.storage.WritableRecordStorage;
import experimentalcode.erich.newdblayer.storage.WritableStorage;

/**
 * Simple factory class that will store all data in memory using object arrays or hashmaps.
 * 
 * Hints are currently not used by this implementation, since everything is in-memory.
 * 
 * @author Erich Schubert
 */
public class MemoryStorageFactory implements StorageFactory {
  @Override
  public <T> WritableStorage<T> makeStorage(DBIDs ids, @SuppressWarnings("unused") int hints, @SuppressWarnings("unused") Class<? super T> dataclass) {
    if (ids instanceof RangeDBIDs) {
      RangeDBIDs range = (RangeDBIDs) ids;
      Object[] data = new Object[range.size()];
      return new ArrayStorage<T>(data, new RangeIDMap(range));
    } else {
      return new MapStorage<T>();
    }
  }

  @Override
  public WritableRecordStorage makeRecordStorage(DBIDs ids, @SuppressWarnings("unused") int hints, Class<?>... dataclasses) {
    if (ids instanceof RangeDBIDs) {
      RangeDBIDs range = (RangeDBIDs) ids;
      Object[][] data = new Object[range.size()][dataclasses.length];
      return new ArrayRecordStorage(data, new RangeIDMap(range));
    } else {
      return new MapRecordStorage(dataclasses.length);
    }
  }
}
