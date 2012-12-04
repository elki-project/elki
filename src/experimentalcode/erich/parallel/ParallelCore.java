package experimentalcode.erich.parallel;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

/**
 * Core for parallel processing in ELKI.
 * 
 * @author Erich Schubert
 */
public class ParallelCore {
  /**
   * The number of CPUs to use.
   */
  public static final int ALL_PROCESSORS = Runtime.getRuntime().availableProcessors();

  /**
   * Static core
   */
  private static final ParallelCore STATIC = new ParallelCore(Runtime.getRuntime().availableProcessors());

  /**
   * Executor service.
   */
  ThreadPoolExecutor executor;

  /**
   * Constructor.
   */
  protected ParallelCore(int processors) {
    super();
    executor = new ThreadPoolExecutor(0, processors, 10, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
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
    return ALL_PROCESSORS;
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
}
