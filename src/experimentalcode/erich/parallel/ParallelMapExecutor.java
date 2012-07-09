package experimentalcode.erich.parallel;

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

/**
 * Class to run mappers in Parallel.
 * 
 * TODO: add progress
 * 
 * @author Erich Schubert
 */
public class ParallelMapExecutor {
  /**
   * Run a task on all available CPUs.
   * 
   * @param ids IDs to process
   * @param mapper Mappers to run
   */
  public final void run(DBIDs ids, Mapper... mapper) {
    // FIXME: use different strategies, depending on the ids type?
    // TODO: try different strategies anyway!
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // TODO: use more segments than processors for better handling runtime
    // differences?
    ParallelCore core = ParallelCore.getCore();
    final int numparts = core.getParallelism();

    List<Future<ArrayDBIDs>> parts = new ArrayList<Future<ArrayDBIDs>>(numparts);
    for(int i = 0; i < numparts; i++) {
      Callable<ArrayDBIDs> run = new InterleavedArrayRunner(aids, i, aids.size(), numparts, mapper);
      parts.add(core.submit(run));
    }

    try {
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
  }

  /**
   * Run for an array, with step size.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf Mapper.Instance
   */
  protected class InterleavedArrayRunner implements Callable<ArrayDBIDs>, MapExecutor {
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
     * Step size
     */
    private int step;

    /**
     * Mapper
     */
    private Mapper.Instance[] mapper;
    
    /**
     * Channel map
     */
    private HashMap<Object, Object> channels = new HashMap<Object, Object>();

    /**
     * Constructor.
     * 
     * @param ids IDs to process
     * @param start Starting position
     * @param end End position
     * @param step Step size
     * @param done Counter to decrement when done.
     */
    protected InterleavedArrayRunner(ArrayDBIDs ids, int start, int end, int step, Mapper[] mapper) {
      super();
      this.ids = ids;
      this.start = start;
      this.end = end;
      this.step = step;
      this.mapper = new Mapper.Instance[mapper.length];
      for (int i = 0; i < mapper.length; i++) {
        this.mapper[i] = mapper[i].instantiate(this);
      }
    }

    @Override
    public ArrayDBIDs call() {
      DBIDArrayIter iter = ids.iter();
      iter.seek(start);
      for(; iter.valid() && iter.getOffset() < end; iter.advance(step)) {
        for (int i = 0; i < mapper.length; i++) {
          mapper[i].map(iter);
        }
        // This is a good moment for multitasking
        Thread.yield();
      }
      return ids;
    }

    @Override
    public <T> InputChannel.Instance<T> instantiate(InputChannel<T> chan) {
      @SuppressWarnings("unchecked")
      InputChannel.Instance<T> chaninst = (InputChannel.Instance<T>) channels.get(chan);
      if (chaninst == null) {
        chaninst = chan.instantiate(this);
        channels.put(chan, chaninst);
      }
      return chaninst;
    }

    @Override
    public <T> OutputChannel.Instance<T> instantiate(OutputChannel<T> chan) {
      @SuppressWarnings("unchecked")
      OutputChannel.Instance<T> chaninst = (OutputChannel.Instance<T>) channels.get(chan);
      if (chaninst == null) {
        chaninst = chan.instantiate(this);
        channels.put(chan, chaninst);
      }
      return chaninst;
    }
  }
}