package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread to render thumbnails in the background.
 * 
 * @author Erich Schubert
 * 
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
    ti.callback.doThumbnail();
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
     */
    public void doThumbnail();
  }
}