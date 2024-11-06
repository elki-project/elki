/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2024
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
package elki.database.ids;

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.IntegerDataStore;
import elki.database.datastore.WritableIntegerDataStore;

/**
 * Generate a static DBID to integer 0:n mapping.
 * 
 * @author Erich Schubert
 */
public class DBIDArrayEnum implements DBIDEnum {
  /**
   * Input IDs
   */
  private ArrayStaticDBIDs ids;

  /**
   * Mapping
   */
  private IntegerDataStore index;

  /**
   * Constructor.
   *
   * @param ids Input ids
   */
  public DBIDArrayEnum(ArrayStaticDBIDs ids) {
    this.ids = ids;
    WritableIntegerDataStore index = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    int j = 0;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      index.put(iter, j++);
    }
    this.index = index;
  }

  @Override
  public int index(DBIDRef dbid) {
    return index.intValue(dbid);
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public DBIDArrayIter iter() {
    return ids.iter();
  }

  @Deprecated
  @Override
  public DBID get(int i) {
    return ids.get(i);
  }

  @Override
  public DBIDVar assignVar(int index, DBIDVar var) {
    return ids.assignVar(index, var);
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return ids.binarySearch(key);
  }

  @Override
  public ArrayDBIDs slice(int begin, int end) {
    return ids.slice(begin, end);
  }

  @Override
  public boolean contains(DBIDRef o) {
    return ids.contains(o);
  }
}
