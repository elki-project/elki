package de.lmu.ifi.dbs.elki.logging.progress;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Class to keep track of "alive" progresses.
 * 
 * @author Erich Schubert
 */
public class ProgressTracker {
  /**
   * Set of potentially active progresses.
   */
  private Vector<WeakReference<Progress>> progresses = new Vector<WeakReference<Progress>>();

  /**
   * Get a list of progresses tracked.
   * 
   * @return List of progresses.
   */
  public synchronized Collection<Progress> getProgresses() {
    List<Progress> list = new ArrayList<Progress>(progresses.size());
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
    progresses.add(new WeakReference<Progress>(p));
  }

  /**
   * Remove completed progresses.
   * 
   * @return List of progresses removed.
   */
  public synchronized Collection<Progress> removeCompleted() {
    List<Progress> list = new ArrayList<Progress>(progresses.size());
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
