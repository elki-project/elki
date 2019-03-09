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
package elki.result;

import java.lang.ref.Reference;
import java.util.ArrayList;

import elki.logging.Logging;

/**
 * Class manage listeners for result changes.
 *
 * @author Erich Schubert
 */
public final class ResultListenerList {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(ResultListenerList.class);

  /**
   * Holds the listener.
   */
  private static ArrayList<ResultListener> listenerList = new ArrayList<>();

  /**
   * Constructor.
   */
  private ResultListenerList() {
    super();
  }

  /**
   * Register a result listener.
   *
   * @param listener Result listener.
   */
  public static void addListener(ResultListener listener) {
    listenerList.add(listener);
  }

  /**
   * Remove a result listener.
   *
   * @param listener Result listener.
   */
  public static void removeListener(ResultListener listener) {
    listenerList.remove(listener);
  }

  /**
   * Informs all registered {@link ResultListener} that a new result was added.
   *
   * @param child New child result added
   * @param parent Parent result that was added to
   */
  public static void resultAdded(Object child, Object parent) {
    child = deref(child);
    parent = deref(parent);
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
  public static void resultChanged(Object current) {
    current = deref(current);
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
  public static void resultRemoved(Object child, Object parent) {
    child = deref(child);
    parent = deref(parent);
    if(LOG.isDebugging()) {
      LOG.debug("Result removed: " + child + " <- " + parent);
    }
    for(int i = listenerList.size(); --i >= 0;) {
      listenerList.get(i).resultRemoved(child, parent);
    }
  }

  /**
   * Automatically expand a reference.
   * 
   * @param ret Object
   * @return Referenced object (may be null!) or ret.
   */
  private static Object deref(Object ret) {
    return (ret instanceof Reference) ? ((Reference<?>) ret).get() : ret;
  }
}
