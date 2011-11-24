package de.lmu.ifi.dbs.elki.database.datastore.memory;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.RangeIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
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
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses ArrayStore oneway - - «create»
 * @apiviz.uses ArrayRecordStore oneway - - «create»
 * @apiviz.uses MapStore oneway - - «create»
 * @apiviz.uses MapRecordStore oneway - - «create»
 */
public class MemoryDataStoreFactory implements DataStoreFactory {
  @Override
  public <T> WritableDataStore<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      Object[] data = new Object[range.size()];
      return new ArrayStore<T>(data, new RangeIDMap(range));
    }
    else {
      return new MapIntegerDBIDStore<T>(ids.size());
    }
  }

  @Override
  public WritableDoubleDataStore makeDoubleStorage(DBIDs ids, int hints) {
    // TODO: add range-double-store.
    return new MapIntegerDBIDDoubleStore(ids.size());
  }

  @Override
  public WritableRecordStore makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses) {
    if(ids instanceof DBIDRange) {
      DBIDRange range = (DBIDRange) ids;
      Object[][] data = new Object[range.size()][dataclasses.length];
      return new ArrayRecordStore(data, new RangeIDMap(range));
    }
    else {
      return new MapIntegerDBIDRecordStore(ids.size(), dataclasses.length);
    }
  }
}