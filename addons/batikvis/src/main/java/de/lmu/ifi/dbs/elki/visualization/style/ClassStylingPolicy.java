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
package de.lmu.ifi.dbs.elki.visualization.style;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Styling policy that is based on <em>classes</em>, for example clusters or
 * labels. This allows for certain optimizations such as marker reuse, and thus
 * is preferred when possible.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public interface ClassStylingPolicy extends StylingPolicy {
  /**
   * Get the style number for a particular object
   *
   * @param id Object ID
   * @return Style number
   */
  int getStyleForDBID(DBIDRef id);

  /**
   * Get the minimum style in use.
   *
   * @return Style number
   */
  int getMinStyle();

  /**
   * Get the maximum style in use.
   *
   * @return Style number
   */
  int getMaxStyle();

  /**
   * Iterate over all objects from a given class.
   *
   * @param cnum Class number
   * @return Iterator over object IDs
   */
  DBIDIter iterateClass(int cnum);

  /**
   * Get the number of elements in the styling class.
   *
   * @param cnum Class number
   * @return Size of class.
   */
  int classSize(int cnum);
}