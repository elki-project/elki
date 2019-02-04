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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedDouble;

/**
 * Abstract base class for processors that output double values.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - Instance
 * @assoc - - - SharedDouble
 */
public abstract class AbstractDoubleProcessor implements Processor {
  /**
   * Output variable
   */
  protected SharedDouble output;

  /**
   * Connect the output variable.
   * 
   * @param output Output variable
   */
  public void connectOutput(SharedDouble output) {
    this.output = output;
  }

  @Override
  public abstract Processor.Instance instantiate(Executor exectutor);

  @Override
  public void cleanup(Processor.Instance inst) {
    // Do nothing by default.
  }

  /**
   * Instance.
   * 
   * @author Erich Schubert
   */
  public static abstract class Instance implements Processor.Instance {
    /**
     * Output variable
     */
    protected SharedDouble.Instance output;

    /**
     * Constructor.
     * 
     * @param output Output variable
     */
    public Instance(SharedDouble.Instance output) {
      super();
      this.output = output;
    }

    @Override
    public abstract void map(DBIDRef id);
  }
}
