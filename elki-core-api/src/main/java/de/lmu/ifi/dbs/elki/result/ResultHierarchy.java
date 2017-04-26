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
package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.EmptyIterator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Class to store a hierarchy of result objects.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
// TODO: add listener merging!
public class ResultHierarchy {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(ResultHierarchy.class);

  /**
   * Holds the listener.
   */
  private ArrayList<ResultListener> listenerList = new ArrayList<>();

  /**
   * Constructor.
   */
  public ResultHierarchy() {
    super();
  }

  public boolean add(Result parent, Result child) {
    if(!Metadata.of(parent).hierarchy().addChild(child)) {
      return false;
    }
    fireResultAdded(child, parent);
    return true;
  }

  public boolean remove(Result parent, Result child) {
    if(!Metadata.of(parent).hierarchy().removeChild(child)) {
      return false;
    }
    fireResultRemoved(child, parent);
    return true;
  }

  public void remove(Result r) {
    for(It<Object> it = Metadata.of(r).hierarchy().iterParentsReverse(); it.valid(); it.advance()) {
      Metadata.of(it.get()).hierarchy().removeChild(r);
    }
  }

  /**
   * Register a result listener.
   *
   * @param listener Result listener.
   */
  public void addResultListener(ResultListener listener) {
    listenerList.add(listener);
  }

  /**
   * Remove a result listener.
   *
   * @param listener Result listener.
   */
  public void removeResultListener(ResultListener listener) {
    listenerList.remove(listener);
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
    if(LOG.isDebugging()) {
      LOG.debug("Result added: " + child + " <- " + parent);
    }
    for(int i = listenerList.size(); --i >= 0;) {
      listenerList.get(i).resultAdded(child, parent);
    }
  }

  /**
   * Informs all registered {@link ResultListener} that a result has changed.
   *
   * @param current Result that has changed
   */
  private void fireResultChanged(Result current) {
    if(LOG.isDebugging()) {
      LOG.debug("Result changed: " + current);
    }
    for(int i = listenerList.size(); --i >= 0;) {
      listenerList.get(i).resultChanged(current);
    }
  }

  /**
   * Informs all registered {@link ResultListener} that a new result has been
   * removed.
   *
   * @param child result that has been removed
   * @param parent Parent result that has been removed
   */
  private void fireResultRemoved(Result child, Result parent) {
    if(LOG.isDebugging()) {
      LOG.debug("Result removed: " + child + " <- " + parent);
    }
    for(int i = listenerList.size(); --i >= 0;) {
      listenerList.get(i).resultRemoved(child, parent);
    }
  }

  public It<Object> iterAll() {
    return EmptyIterator.empty();
  }
}
