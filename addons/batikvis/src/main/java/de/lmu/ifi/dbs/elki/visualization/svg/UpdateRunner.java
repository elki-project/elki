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
package de.lmu.ifi.dbs.elki.visualization.svg;

import java.util.concurrent.ConcurrentLinkedQueue;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Class to handle updates to an SVG plot, in particular when used in an Apache
 * Batik UI.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @has - - - Runnable
 * @assoc - - - UpdateSynchronizer
 */
public class UpdateRunner {
  /**
   * Owner/Synchronization object
   */
  private Object sync;

  /**
   * The queue of pending updates
   */
  final private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

  /**
   * Synchronizer that can block events from being executed right away.
   */
  private UpdateSynchronizer synchronizer = null;

  /**
   * Construct a new update handler
   *
   * @param sync Object to synchronize on
   */
  protected UpdateRunner(Object sync) {
    this.sync = sync;
  }

  /**
   * Add a new update to run at any appropriate time.
   *
   * @param r New runnable to perform the update
   */
  public void invokeLater(Runnable r) {
    queue.add(r);
    synchronized(this) {
      if(synchronizer == null) {
        runQueue();
      }
      else {
        synchronizer.activate();
      }
    }
  }

  /**
   * Run the processing queue now. This should usually be only invoked by the
   * UpdateSynchronizer
   */
  public void runQueue() {
    synchronized(sync) {
      while(!queue.isEmpty()) {
        Runnable r = queue.poll();
        if(r != null) {
          try {
            r.run();
          }
          catch(Exception e) {
            // Alternatively, we could allow the specification of exception
            // handlers for each runnable in the API. For now we'll just log.
            // TODO: handle exceptions here better!
            LoggingUtil.exception(e);
          }
        }
        else {
          LoggingUtil.warning("Tried to run a 'null' Object.");
        }
      }
    }
  }

  /**
   * Clear queue. For shutdown!
   */
  public synchronized void clear() {
    queue.clear();
  }

  /**
   * Check whether the queue is empty.
   *
   * @return queue status
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Set a new update synchronizer.
   *
   * @param newsync Update synchronizer
   */
  public synchronized void synchronizeWith(UpdateSynchronizer newsync) {
    // LoggingUtil.warning("Synchronizing: " + sync + " " + newsync, new
    // Throwable());
    if(synchronizer == newsync) {
      LoggingUtil.warning("Double-synced to the same plot!", new Throwable());
      return;
    }
    if(synchronizer != null) {
      LoggingUtil.warning("Attempting to synchronize to more than one synchronizer.");
      return;
    }
    synchronizer = newsync;
    newsync.addUpdateRunner(this);
  }

  /**
   * Remove an update synchronizer
   *
   * @param oldsync Update synchronizer to remove
   */
  public synchronized void unsynchronizeWith(UpdateSynchronizer oldsync) {
    if(synchronizer == null) {
      LoggingUtil.warning("Warning: was not synchronized.");
      return;
    }
    if(synchronizer != oldsync) {
      LoggingUtil.warning("Warning: was synchronized differently!");
      return;
    }
    // LoggingUtil.warning("Unsynchronizing: " + sync + " " + oldsync);
    synchronizer = null;
    runQueue();
  }
}