/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.database.datastore.memory;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Simple factory class that will store all data in memory using object arrays
 * or hashmaps.
 * 
 * Hints are currently not used by this implementation, since everything is
 * in-memory.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navhas - create - ArrayStore
 * @navhas - create - ArrayRecordStore
 * @navhas - create - MapStore
 * @navhas - create - MapRecordStore
 */
public class MemoryDataStoreFactory implements DataStoreFactory {
  @SuppressWarnings("unchecked")
  @Override
  public <T> WritableDataStore<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass) {
    if (Double.class.equals(dataclass)) {
      return (WritableDataStore<T>) makeDoubleStorage(ids, hints);
    }
    if (Integer.class.equals(dataclass)) {
      return (WritableDataStore<T>) makeIntegerStorage(ids, hints);
    }
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      Object[] data = new Object[range.size()];
      return new ArrayStore<>(data, range);
    }
    else {
      return new MapIntegerDBIDStore<>(ids.size());
    }
  }

  @Override
  public WritableDBIDDataStore makeDBIDStorage(DBIDs ids, int hints) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      return new ArrayDBIDStore(range.size(), range);
    }
    else {
      return new MapIntegerDBIDDBIDStore(ids.size());
    }
  }

  @Override
  public WritableDoubleDataStore makeDoubleStorage(DBIDs ids, int hints) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      return new ArrayDoubleStore(range.size(), range);
    }
    else {
      return new MapIntegerDBIDDoubleStore(ids.size());
    }
  }

  @Override
  public WritableDoubleDataStore makeDoubleStorage(DBIDs ids, int hints, double def) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      return new ArrayDoubleStore(range.size(), range, def);
    }
    else {
      return new MapIntegerDBIDDoubleStore(ids.size(), def);
    }
  }

  @Override
  public WritableIntegerDataStore makeIntegerStorage(DBIDs ids, int hints) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      return new ArrayIntegerStore(range.size(), range);
    }
    else {
      return new MapIntegerDBIDIntegerStore(ids.size());
    }
  }

  @Override
  public WritableIntegerDataStore makeIntegerStorage(DBIDs ids, int hints, int def) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      return new ArrayIntegerStore(range.size(), range, def);
    }
    else {
      return new MapIntegerDBIDIntegerStore(ids.size(), def);
    }
  }

  @Override
  public WritableRecordStore makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      Object[][] data = new Object[range.size()][dataclasses.length];
      return new ArrayRecordStore(data, range);
    }
    else {
      return new MapIntegerDBIDRecordStore(ids.size(), dataclasses.length);
    }
  }
}
