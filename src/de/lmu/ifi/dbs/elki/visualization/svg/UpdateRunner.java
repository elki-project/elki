package de.lmu.ifi.dbs.elki.visualization.svg;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Class to handle updates to an SVG plot, in particular when used in an Apache
 * Batik UI.
 * 
 * @author Erich Schubert
 */
public class UpdateRunner {
  /**
   * Owner/Synchronization object
   */
  private Object sync;

  /**
   * The queue of pending updates
   */
  final private Queue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();

  /**
   * Synchronizer that can block events from being executed right away.
   */
  private Collection<UpdateSynchronizer> synchronizer = new java.util.Vector<UpdateSynchronizer>();

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
      if(synchronizer.size() > 0) {
        for(UpdateSynchronizer s : synchronizer) {
          s.activate();
        }
      }
      else {
        synchronized(sync) {
          runQueue();
        }
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
        try {
          r.run();
        }
        catch(Exception e) {
          // Alternatively, we could allow the specification of exception
          // handlers
          // for each runnable in the API. For now we'll just log.
          // TODO: handle exceptions here better!
          LoggingUtil.exception(e);
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
   * @param sync Update synchronizer
   */
  public synchronized void synchronizeWith(UpdateSynchronizer newsync) {
    synchronized(sync) {
      if(synchronizer.contains(newsync)) {
        LoggingUtil.warning("Double-synced to the same plot!");
      }
      else {
        synchronizer.add(newsync);
        newsync.addUpdateRunner(this);
      }
    }
  }

  /**
   * Remove an update synchronizer
   * 
   * @param oldsync Update synchronizer to remove
   */
  public synchronized void unsynchronizeWith(UpdateSynchronizer oldsync) {
    synchronized(sync) {
      synchronizer.remove(oldsync);
      if(synchronizer.size() == 0) {
        runQueue();
      }
    }
  }
}