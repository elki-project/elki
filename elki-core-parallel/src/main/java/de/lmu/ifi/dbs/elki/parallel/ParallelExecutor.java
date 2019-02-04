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
import de.lmu.ifi.dbs.elki.parallel.processor.Processor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable.Instance;

/**
 * Class to run processors in parallel, on all available cores.
 *
 * TODO: add progress
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - BlockArrayRunner
 * @assoc - - - ParallelCore
 */
public final class ParallelExecutor {
  /**
   * Private constructor. Static methods only.
   */
  private ParallelExecutor() {
    // Do not use.
  }

  /**
   * Run a task on all available CPUs.
   *
   * @param ids IDs to process
   * @param procs Processors to run
   */
  public static void run(DBIDs ids, Processor... procs) {
    ParallelCore core = ParallelCore.getCore();
    core.connect();
    try {
      // TODO: try different strategies anyway!
      ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
      final int size = aids.size();
      int numparts = core.getParallelism();
      // TODO: are there better heuristics for choosing this?
      numparts = (size > numparts * numparts * 16) ? numparts * Math.max(1, numparts - 1) : numparts;

      final int blocksize = (size + (numparts - 1)) / numparts;
      List<Future<ArrayDBIDs>> parts = new ArrayList<>(numparts);
      for(int i = 0; i < numparts; i++) {
        final int start = i * blocksize;
        final int end = Math.min(start + blocksize, size);
        Callable<ArrayDBIDs> run = new BlockArrayRunner(aids, start, end, procs);
        parts.add(core.submit(run));
      }

      for(Future<ArrayDBIDs> fut : parts) {
        fut.get();
      }
    }
    catch(ExecutionException e) {
      throw new RuntimeException("Processor execution failed.", e);
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
   * @assoc - - - Processor
   */
  protected static class BlockArrayRunner implements Callable<ArrayDBIDs>, Executor {
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
     * The processor masters that own the instances.
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
     * @param start Starting position
     * @param end End position
     * @param procs Processors to run
     */
    protected BlockArrayRunner(ArrayDBIDs ids, int start, int end, Processor[] procs) {
      super();
      this.ids = ids;
      this.start = start;
      this.end = end;
      this.procs = procs;
    }

    @Override
    public ArrayDBIDs call() {
      Processor.Instance[] instances = new Processor.Instance[procs.length];
      for(int i = 0; i < procs.length; i++) {
        instances[i] = procs[i].instantiate(this);
      }
      for(DBIDArrayIter iter = ids.iter().seek(start); iter.valid() && iter.getOffset() < end; iter.advance()) {
        for(int i = 0; i < instances.length; i++) {
          instances[i].map(iter);
        }
      }
      for(int i = 0; i < instances.length; i++) {
        procs[i].cleanup(instances[i]);
      }
      return ids;
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
