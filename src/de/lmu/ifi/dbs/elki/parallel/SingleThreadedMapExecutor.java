package de.lmu.ifi.dbs.elki.parallel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.parallel.mapper.Mapper;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable.Instance;

/**
 * Class to process the whole data set in a single thread.
 * 
 * Currently not used.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has SingleThreadedRunner
 */
public class SingleThreadedMapExecutor {
  /**
   * Run a task on a single thread.
   * 
   * @param ids IDs to process
   * @param mapper Mappers to run
   */
  public static final void run(DBIDs ids, Mapper... mapper) {
    new SingleThreadedRunner(ids, mapper).run();
  }

  /**
   * Run for an array part, without step size.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses Mapper
   */
  protected static class SingleThreadedRunner implements MapExecutor {
    /**
     * Array IDs to process
     */
    private DBIDs ids;

    /**
     * The mapper masters that own the instances.
     */
    private Mapper[] mapper;

    /**
     * Variables map.
     */
    private HashMap<SharedVariable<?>, SharedVariable.Instance<?>> variables = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param ids IDs to process
     * @param mapper Mapper functions to run
     */
    protected SingleThreadedRunner(DBIDs ids, Mapper[] mapper) {
      super();
      this.ids = ids;
      this.mapper = mapper;
    }

    public void run() {
      Mapper.Instance[] instances = new Mapper.Instance[mapper.length];
      for(int i = 0; i < mapper.length; i++) {
        instances[i] = mapper[i].instantiate(this);
      }

      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        for(int i = 0; i < instances.length; i++) {
          instances[i].map(iter);
        }
      }
      for(int i = 0; i < instances.length; i++) {
        mapper[i].cleanup(instances[i]);
      }
    }

    @Override
    public <I extends Instance<?>> I getInstance(SharedVariable<I> parent) {
      @SuppressWarnings("unchecked")
      I inst = (I) variables.get(parent);
      if(inst == null) {
        inst = parent.instantiate();
        variables.put(parent, inst);
      }
      return inst;
    }
  }
}
