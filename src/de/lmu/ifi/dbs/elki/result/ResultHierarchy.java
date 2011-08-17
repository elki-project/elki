package de.lmu.ifi.dbs.elki.result;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.logging.Logging;
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
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ResultHierarchy.class);
  
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
    if(logger.isDebugging()) {
      logger.debug("Result added: " + child + " <- " + parent);
    }
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
    if(logger.isDebugging()) {
      logger.debug("Result changed: " + current);
    }
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
    if(logger.isDebugging()) {
      logger.debug("Result removed: " + child + " <- " + parent);
    }
    for(ResultListener l : listenerList.getListeners(ResultListener.class)) {
      l.resultRemoved(child, parent);
    }
  }
}