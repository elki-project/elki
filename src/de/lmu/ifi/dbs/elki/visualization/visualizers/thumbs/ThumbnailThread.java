package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;

/**
 * Thread to render thumbnails in the background.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Thumbnailer
 * @apiviz.uses Listener oneway - - signals
 */
public class ThumbnailThread extends Thread {
  /**
   * Queue of thumbnails to generate.
   */
  private Queue<Task> queue = new ConcurrentLinkedQueue<Task>();

  /**
   * Flag to signal shutdown.
   */
  private boolean shutdown = false;

  /**
   * Thumbnailer to use.
   */
  private Thumbnailer t = new Thumbnailer();

  /**
   * The static thumbnail thread.
   */
  private static ThumbnailThread THREAD = null;

  /**
   * Queue a thumbnail task in a global thumbnail thread.
   * 
   * @param callback Callback
   */
  public synchronized static Task QUEUE(Listener callback) {
    final Task task = new Task(callback);
    if(THREAD != null) {
      // TODO: synchronization?
      if(THREAD.isAlive()) {
        THREAD.queue(task);
        return task;
      }
    }
    THREAD = new ThumbnailThread();
    THREAD.queue(task);
    THREAD.start();
    return task;
  }

  /**
   * Remove a pending task from the queue.
   * 
   * @param task Task to remove.
   */
  public static void UNQUEUE(Task task) {
    if(THREAD != null) {
      synchronized(THREAD) {
        THREAD.queue.remove(task);
      }
    }
  }

  /**
   * Shutdown the thumbnailer thread.
   */
  public static synchronized void SHUTDOWN() {
    if(THREAD != null && THREAD.isAlive()) {
      THREAD.shutdown();
    }
  }

  /**
   * Queue a new thumbnail task.
   * 
   * @param task Thumbnail task
   */
  private void queue(Task task) {
    this.queue.add(task);
  }

  /**
   * Generate a single Thumbnail.
   * 
   * @param ti Visualization task
   */
  private void generateThumbnail(Task ti) {
    ti.callback.doThumbnail(t);
  }

  @Override
  public void run() {
    while(!queue.isEmpty() && !shutdown) {
      generateThumbnail(queue.poll());
    }
  }

  /**
   * Set the shutdown flag.
   */
  private void shutdown() {
    this.shutdown = true;
    queue.clear();
  }

  /**
   * A single thumbnailer task.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Task {
    /**
     * Runnable to call back
     */
    Listener callback;

    /**
     * Constructor.
     * 
     * @param callback Callback when complete
     */
    public Task(Listener callback) {
      super();
      this.callback = callback;
    }
  }

  /**
   * Listener interface for completed thumbnails.
   * 
   * @author Erich Schubert
   */
  public interface Listener {
    /**
     * Callback when to (re-)compute the thumbnail.
     * 
     * @param t Thumbnailer to use
     */
    public void doThumbnail(Thumbnailer t);
  }
}