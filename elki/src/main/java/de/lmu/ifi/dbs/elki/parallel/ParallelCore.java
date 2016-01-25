package de.lmu.ifi.dbs.elki.parallel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core for parallel processing in ELKI, based on {@link ThreadPoolExecutor}.
 * 
 * TODO: make configurable how many threads are used.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ParallelCore {
  /**
   * The number of CPUs to use.
   */
  public static final int ALL_PROCESSORS = Runtime.getRuntime().availableProcessors();

  /**
   * Static core
   */
  private static final ParallelCore STATIC = new ParallelCore(ALL_PROCESSORS);

  /**
   * Executor service.
   */
  ThreadPoolExecutor executor;

  /**
   * Number of connected submitters.
   */
  private AtomicInteger connected = new AtomicInteger(0);

  /**
   * Maximum number of processors to use.
   */
  private int processors;

  /**
   * Constructor.
   */
  protected ParallelCore(int processors) {
    super();
    this.processors = processors;
  }

  /**
   * Get the static core object.
   * 
   * @return Core
   */
  public static ParallelCore getCore() {
    return STATIC;
  }

  /**
   * Get desired level of parallelism
   * 
   * @return Number of threads to run in parallel
   */
  public int getParallelism() {
    return executor.getMaximumPoolSize();
  }

  /**
   * Submit a task to the executor core.
   * 
   * @param task Submitted task
   * 
   * @return Future to observe completion
   */
  public <T> Future<T> submit(Callable<T> task) {
    return executor.submit(task);
  }

  /**
   * Connect to the executor.
   */
  public void connect() {
    if(executor == null) {
      synchronized(this) {
        if(executor == null) {
          executor = new ThreadPoolExecutor(0, processors, 10L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
          executor.allowCoreThreadTimeOut(true);
        }
      }
    }
    int c = this.connected.incrementAndGet();
    if(c == 1) {
      executor.allowCoreThreadTimeOut(false);
      executor.setCorePoolSize(executor.getMaximumPoolSize());
    }
  }

  /**
   * Disconnect to the executor.
   */
  public void disconnect() {
    int c = this.connected.decrementAndGet();
    if(c == 0) {
      synchronized(this) {
        executor.allowCoreThreadTimeOut(true);
        executor.setCorePoolSize(0);
      }
    }
  }
}
