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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.parallel.mapper.Mapper;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable.Instance;

/**
 * Class to run mappers in parallel, on all available cores.
 * 
 * TODO: add progress
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has BlockArrayRunner
 * @apiviz.uses ParallelCore
 */
public class ParallelMapExecutor {
  /**
   * Run a task on all available CPUs.
   * 
   * @param ids IDs to process
   * @param mapper Mappers to run
   */
  public static final void run(DBIDs ids, Mapper... mapper) {
    // TODO: try different strategies anyway!
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    ParallelCore core = ParallelCore.getCore();
    try {
      final int size = aids.size();
      core.connect();
      int numparts = core.getParallelism();
      // TODO: are there better heuristics for choosing this?
      numparts = (size > numparts * numparts * 16) ? numparts * numparts - 1 : numparts;

      final int blocksize = (size + (numparts - 1)) / numparts;
      List<Future<ArrayDBIDs>> parts = new ArrayList<>(numparts);
      for(int i = 0; i < numparts; i++) {
        final int start = i * blocksize;
        final int end = (start + blocksize < size) ? start + blocksize : size;
        Callable<ArrayDBIDs> run = new BlockArrayRunner(aids, start, end, mapper);
        parts.add(core.submit(run));
      }

      for(Future<ArrayDBIDs> fut : parts) {
        fut.get();
      }
    }
    catch(ExecutionException e) {
      throw new RuntimeException("Mapper execution failed.", e);
    }
    catch(InterruptedException e) {
      throw new RuntimeException("Parallel execution interrupted.");
    }
    finally {
      core.disconnect();
    }
  }

  /**
   * Run for an array part, without step size.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses Mapper
   */
  protected static class BlockArrayRunner implements Callable<ArrayDBIDs>, MapExecutor {
    /**
     * Array IDs to process
     */
    private ArrayDBIDs ids;

    /**
     * Start position
     */
    private int start;

    /**
     * End position
     */
    private int end;

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
     * @param start Starting position
     * @param end End position
     * @param mapper Mapper functions to run
     */
    protected BlockArrayRunner(ArrayDBIDs ids, int start, int end, Mapper[] mapper) {
      super();
      this.ids = ids;
      this.start = start;
      this.end = end;
      this.mapper = mapper;
    }

    @Override
    public ArrayDBIDs call() {
      Mapper.Instance[] instances = new Mapper.Instance[mapper.length];
      for(int i = 0; i < mapper.length; i++) {
        instances[i] = mapper[i].instantiate(this);
      }

      DBIDArrayIter iter = ids.iter();
      iter.seek(start);
      for(int c = end - start; iter.valid() && c >= 0; iter.advance(), c--) {
        for(int i = 0; i < instances.length; i++) {
          instances[i].map(iter);
        }
      }
      for(int i = 0; i < instances.length; i++) {
        mapper[i].cleanup(instances[i]);
      }
      return ids;
    }

    @Override
    public <I extends Instance<?>> I getInstance(SharedVariable<I> parent) {
      @SuppressWarnings("unchecked")
      I inst = (I) variables.get(parent);
      if (inst == null) {
        inst = parent.instantiate();
        variables.put(parent, inst);
      }
      return inst;
    }
  }
}
