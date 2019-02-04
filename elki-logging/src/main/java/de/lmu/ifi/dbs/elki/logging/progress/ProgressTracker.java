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
package de.lmu.ifi.dbs.elki.logging.progress;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class to keep track of "alive" progresses.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @navassoc - tracks - Progress
 */
public class ProgressTracker {
  /**
   * Set of potentially active progresses.
   */
  private ArrayList<WeakReference<Progress>> progresses = new ArrayList<>();

  /**
   * Get a list of progresses tracked.
   * 
   * @return List of progresses.
   */
  public synchronized Collection<Progress> getProgresses() {
    List<Progress> list = new ArrayList<>(progresses.size());
    Iterator<WeakReference<Progress>> iter = progresses.iterator();
    while(iter.hasNext()) {
      WeakReference<Progress> ref = iter.next();
      if(ref.get() == null) {
        iter.remove();
      }
      else {
        list.add(ref.get());
      }
    }
    return list;
  }

  /**
   * Add a new Progress to the tracker.
   * 
   * @param p Progress
   */
  public synchronized void addProgress(Progress p) {
    // Don't add more than once.
    Iterator<WeakReference<Progress>> iter = progresses.iterator();
    while(iter.hasNext()) {
      WeakReference<Progress> ref = iter.next();
      // since we are at it anyway, remove old links.
      if(ref.get() == null) {
        iter.remove();
      }
      else {
        if(ref.get() == p) {
          return;
        }
      }
    }
    progresses.add(new WeakReference<>(p));
  }

  /**
   * Remove completed progresses.
   * 
   * @return List of progresses removed.
   */
  public synchronized Collection<Progress> removeCompleted() {
    List<Progress> list = new ArrayList<>(progresses.size());
    Iterator<WeakReference<Progress>> iter = progresses.iterator();
    while(iter.hasNext()) {
      WeakReference<Progress> ref = iter.next();
      if(ref.get() == null) {
        iter.remove();
      }
      else {
        if(ref.get().isComplete()) {
          list.add(ref.get());
          iter.remove();
        }
      }
    }
    return list;
  }
}
