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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;

/**
 * Sink collecting minimum and maximum values.
 * 
 * @author Erich Schubert
 */
public class DoubleMinMaxMapper implements Mapper {
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
  public DoubleMinMaxMapper() {
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
  public Instance instantiate(MapExecutor mapper) {
    return new Instance(input.instantiate(mapper));
  }

  /**
   * Merge the result of a mapper thread.
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
  private class Instance implements Mapper.Instance {
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

    @Override
    public void cleanup() {
      merge(minmax);
    }
  }
}