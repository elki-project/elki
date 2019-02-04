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
package de.lmu.ifi.dbs.elki.parallel;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.parallel.processor.Processor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable.Instance;

/**
 * Class to process the whole data set in a single thread.
 * 
 * Currently not used.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - SingleThreadedRunner
 */
public final class SingleThreadedExecutor {
  /**
   * Private constructor. Static methods only.
   */
  private SingleThreadedExecutor() {
    // Do not use.
  }

  /**
   * Run a task on a single thread.
   * 
   * @param ids IDs to process
   * @param procs Processors to run
   */
  public static void run(DBIDs ids, Processor... procs) {
    new SingleThreadedRunner(ids, procs).run();
  }

  /**
   * Run for an array part, without step size.
   * 
   * @author Erich Schubert
   */
  protected static class SingleThreadedRunner implements Executor {
    /**
     * Array IDs to process
     */
    private DBIDs ids;

    /**
     * The process masters that own the instances.
     */
    private Processor[] procs;

    /**
     * Variables map.
     */
    private HashMap<SharedVariable<?>, SharedVariable.Instance<?>> variables = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param ids IDs to process
     * @param procs Processor functions to run
     */
    protected SingleThreadedRunner(DBIDs ids, Processor[] procs) {
      super();
      this.ids = ids;
      this.procs = procs;
    }

    public void run() {
      Processor.Instance[] instances = new Processor.Instance[procs.length];
      for(int i = 0; i < procs.length; i++) {
        instances[i] = procs[i].instantiate(this);
      }

      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        for(int i = 0; i < instances.length; i++) {
          instances[i].map(iter);
        }
      }
      for(int i = 0; i < instances.length; i++) {
        procs[i].cleanup(instances[i]);
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
