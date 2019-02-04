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
package de.lmu.ifi.dbs.elki.parallel.processor;

import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedDouble;

/**
 * Write double values into a {@link WritableDoubleDataStore}.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - Instance
 * @assoc - - - SharedDouble
 * @assoc - - - WritableDoubleDataStore
 */
public class WriteDoubleDataStoreProcessor implements Processor {
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
  public WriteDoubleDataStoreProcessor(WritableDoubleDataStore store) {
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
  public Instance instantiate(Executor executor) {
    return new Instance(executor.getInstance(input));
  }

  @Override
  public void cleanup(Processor.Instance inst) {
    // Nothing to do.
  }

  /**
   * Instance for a sub-channel.
   * 
   * @author Erich Schubert
   */
  public class Instance implements Processor.Instance {
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
      store.putDouble(id, input.doubleValue());
    }
  }
}
