package experimentalcode.erich.parallel.mapper;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;

/**
 * Abstract base class for double mappers.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractDoubleMapper implements Mapper {
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
  public abstract Mapper.Instance instantiate(MapExecutor exectutor);

  @Override
  public void cleanup(Mapper.Instance inst) {
    // Do nothing by default.
  }

  /**
   * Mapper instance.
   * 
   * @author Erich Schubert
   */
  public static abstract class Instance implements Mapper.Instance {
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
