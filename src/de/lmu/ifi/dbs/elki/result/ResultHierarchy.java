package de.lmu.ifi.dbs.elki.result;

import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyHashmapList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.ModifiableHierarchy;

/**
 * Class to store a hierarchy of result objects.
 * 
 * @author Erich Schubert
 */
// TODO: add listener merging!
public class ResultHierarchy extends HierarchyHashmapList<Result> {
  /**
   * Holds the listener.
   */
  private EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor.
   */
  public ResultHierarchy() {
    super();
  }

  @Override
  public void add(Result parent, Result child) {
    super.add(parent, child);
    // TODO: fire listeners
    if(child instanceof HierarchicalResult) {
      HierarchicalResult hr = (HierarchicalResult) child;
      ModifiableHierarchy<Result> h = hr.getHierarchy();
      // Merge hierarchy
      hr.setHierarchy(this);
      // Add children of child
      for(Result desc : h.getChildren(hr)) {
        this.add(hr, desc);
        if(desc instanceof HierarchicalResult) {
          ((HierarchicalResult) desc).setHierarchy(this);
        }
      }
    }
    fireResultAdded(child, parent);
  }

  @SuppressWarnings("unused")
  @Override
  public void remove(Result parent, Result child) {
    // TODO: unlink from hierarchy, fire event
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unused")
  @Override
  public void put(Result obj, List<Result> parents, List<Result> children) {
    // TODO: can we support this somehow? Or reduce visibility?
    throw new UnsupportedOperationException();
  }

  /**
   * Register a result listener.
   * 
   * @param listener Result listener.
   */
  public void addResultListener(ResultListener listener) {
    listenerList.add(ResultListener.class, listener);
  }

  /**
   * Remove a result listener.
   * 
   * @param listener Result listener.
   */
  public void removeResultListener(ResultListener listener) {
    listenerList.remove(ResultListener.class, listener);
  }

  /**
   * Signal that a result has changed (public API)
   * 
   * @param res Result that has changed.
   */
  public void resultChanged(Result res) {
    fireResultChanged(res);
  }

  /**
   * Informs all registered {@link ResultListener} that a new result was added.
   * 
   * @param child New child result added
   * @param parent Parent result that was added to
   */
  private void fireResultAdded(Result child, Result parent) {
    for(ResultListener l : listenerList.getListeners(ResultListener.class)) {
      l.resultAdded(child, parent);
    }
  }

  /**
   * Informs all registered {@link ResultListener} that a result has changed.
   * 
   * @param current Result that has changed
   */
  private void fireResultChanged(Result current) {
    for(ResultListener l : listenerList.getListeners(ResultListener.class)) {
      l.resultChanged(current);
    }
  }

  /**
   * Informs all registered {@link ResultListener} that a new result has been
   * removed.
   * 
   * @param child result that has been removed
   * @param parent Parent result that has been removed
   */
  @SuppressWarnings("unused")
  private void fireResultRemoved(Result child, Result parent) {
    for(ResultListener l : listenerList.getListeners(ResultListener.class)) {
      l.resultRemoved(child, parent);
    }
  }
}