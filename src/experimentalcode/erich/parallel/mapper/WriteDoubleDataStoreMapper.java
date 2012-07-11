package experimentalcode.erich.parallel.mapper;

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

import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;

/**
 * Mapper to write double values into a {@link WritableDoubleDataStore}.
 * 
 * @author Erich Schubert
 */
public class WriteDoubleDataStoreMapper implements Mapper {
  /**
   * Store to write to
   */
  WritableDoubleDataStore store;

  /**
   * Shared double variable
   */
  SharedDouble input;

  /**
   * Constructor.
   * 
   * @param store Data store to write to
   */
  public WriteDoubleDataStoreMapper(WritableDoubleDataStore store) {
    super();
    this.store = store;
  }

  /**
   * Connect the input variable
   * 
   * @param input Input variable
   */
  public void connectInput(SharedDouble input) {
    this.input = input;
  }

  @Override
  public Instance instantiate(MapExecutor mapper) {
    return new Instance(input.instantiate(mapper));
  }

  /**
   * Instance for a sub-channel.
   * 
   * @author Erich Schubert
   */
  public class Instance implements Mapper.Instance {
    /**
     * Shared double variable
     */
    SharedDouble.Instance input;

    /**
     * Constructor.
     * 
     * @param input Input
     */
    public Instance(SharedDouble.Instance input) {
      super();
      this.input = input;
    }

    @Override
    public void map(DBIDRef id) {
      store.put(id, input.doubleValue());
    }

    @Override
    public void cleanup() {
      // Nothing to do
    }
  }
}