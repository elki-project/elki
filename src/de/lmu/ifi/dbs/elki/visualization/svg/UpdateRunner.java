package de.lmu.ifi.dbs.elki.visualization.svg;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Class to handle updates to an SVG plot, in particular when used in an Apache Batik UI.
 * 
 * @author Erich Schubert
 */
public class UpdateRunner {
  /**
   * The queue of pending updates
   */
  final private Queue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
  
  /**
   * Synchronizer that can block events from being executed right away.
   */
  private UpdateSynchronizer synchronizer = null;

  /**
   * Construct a new update handler
   */
  protected UpdateRunner() {
    // nothing to do here.
  }

  /**
   * Add a new update to run at any appropriate time.
   * 
   * @param r New runnable to perform the update
   */
  public synchronized void invokeLater(Runnable r) {
    queue.add(r);
    if (synchronizer != null) {
      synchronizer.activate();
    } else {
      runQueue();
    }
  }

  /**
   * Run the processing queue now. This should usually be only invoked by the UpdateSynchronizer
   */
  public synchronized void runQueue() {
    while(!queue.isEmpty()) {
      Runnable r = queue.poll();
      try {
        r.run();
      }
      catch(Exception e) {
        // Alternatively, we could allow the specification of exception handlers
        // for each runnable in the API. For now we'll just log.
        // TODO: handle exceptions here better!
        LoggingUtil.exception(e);
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
  public synchronized boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Set a new update synchronizer. May be null, to unset.
   * 
   * @param sync Update synchronizer
   */
  public synchronized void setUpdateSynchronizer(UpdateSynchronizer sync) {
    if (synchronizer != null) {
      synchronizer.setUpdateRunner(null);
    }
    synchronizer = sync;
    if (synchronizer != null) {
      synchronizer.setUpdateRunner(this);
    } else {
      runQueue();
    }
  }
}