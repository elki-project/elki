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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedDouble;

/**
 * Sink collecting minimum and maximum values.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - Instance
 * @assoc - - - SharedDouble
 * @has - - - DoubleMinMax
 */
public class DoubleMinMaxProcessor implements Processor {
  /**
   * The central data store.
   */
  DoubleMinMax minmax = new DoubleMinMax();

  /**
   * Input channel
   */
  SharedDouble input;

  /**
   * Constructor.
   */
  public DoubleMinMaxProcessor() {
    super();
  }

  /**
   * Connect an input channel.
   * 
   * @param input Input channel
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
    merge(((Instance) inst).minmax);
  }

  /**
   * Merge the result of an instance.
   * 
   * @param minmax Minmax value
   */
  protected synchronized void merge(DoubleMinMax minmax) {
    this.minmax.put(minmax.getMin());
    this.minmax.put(minmax.getMax());
  }

  /**
   * Get the minmax object.
   * 
   * @return Minmax object
   */
  public DoubleMinMax getMinMax() {
    return minmax;
  }

  /**
   * Instance for a particular sub-channel / part of the data set.
   * 
   * @author Erich Schubert
   */
  private static class Instance implements Processor.Instance {
    /**
     * The central data store.
     */
    private DoubleMinMax minmax = new DoubleMinMax();

    /**
     * Input channel instance
     */
    private SharedDouble.Instance input;

    /**
     * Constructor.
     * 
     * @param input Input channel instance.
     */
    public Instance(SharedDouble.Instance input) {
      super();
      this.input = input;
    }

    @Override
    public void map(DBIDRef id) {
      minmax.put(input.doubleValue());
    }
  }
}
