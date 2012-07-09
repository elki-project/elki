package experimentalcode.erich.parallel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Output channel to store data in a {@link WritableDataStore}.
 * 
 * @author Erich Schubert
 * 
 * @param <T> data type
 */
public class DataStoreOutputChannel<T> implements OutputChannel<T> {
  /**
   * Store to write to
   */
  WritableDataStore<T> store;

  /**
   * Constructor.
   *
   * @param store Data store to write to
   */
  public DataStoreOutputChannel(WritableDataStore<T> store) {
    super();
    this.store = store;
  }

  @Override
  public Instance instantiate(MapExecutor mapper) {
    return new Instance();
  }

  /**
   * Instance for a sub-channel.
   * 
   * @author Erich Schubert
   */
  public class Instance implements OutputChannel.Instance<T> {
    @Override
    public void put(DBIDRef id, T data) {
      store.put(id, data);
    }
  }
}