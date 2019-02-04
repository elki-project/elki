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

import java.util.EventListener;

/**
 * Listener interface invoked when new results are added to the result tree.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - Result
 */
public interface ResultListener extends EventListener {
  /**
   * A new derived result was added.
   * 
   * @param child New child result added
   * @param parent Parent result that was added to
   */
  void resultAdded(Result child, Result parent);
  
  /**
   * Notify that the current result has changed substantially.
   * 
   * @param current Result that has changed.
   */
  void resultChanged(Result current);
  
  /**
   * A result was removed.
   * 
   * @param child result that was removed
   * @param parent Parent result that was removed from
   */
  void resultRemoved(Result child, Result parent);
}
