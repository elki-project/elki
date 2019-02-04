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
package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread to render thumbnails in the background.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - signals - Listener
 */
public class ThumbnailThread extends Thread {
  /**
   * Queue of thumbnails to generate.
   */
  private Queue<Task> queue = new ConcurrentLinkedQueue<>();

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
  public synchronized static Task queue(Listener callback) {
    final Task task = new Task(callback);
    // TODO: synchronization?
    if(THREAD != null && THREAD.isAlive()) {
      THREAD.queue.add(task);
      return task;
    }
    THREAD = new ThumbnailThread();
    THREAD.queue.add(task);
    THREAD.start();
    return task;
  }

  /**
   * Remove a pending task from the queue.
   * 
   * @param task Task to remove.
   */
  public static void unqueue(Task task) {
    if(THREAD != null) {
      synchronized(THREAD) {
        THREAD.queue.remove(task);
      }
    }
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
   * A single thumbnailer task.
   * 
   * @author Erich Schubert
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
    void doThumbnail();
  }
}