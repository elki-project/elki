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
package de.lmu.ifi.dbs.elki.utilities.datastructures.iterator;

/**
 * Modifiable iterator, that also supports removal.
 * 
 * Usage example:
 * 
 * <pre>
 * {@code 
 * for (MIter iter = ids.iter(); iter.valid(); iter.advance()) {
 *   if (testSomething(iter)) {
 *     iter.remove();
 *     continue; // Iterator may point to something else
 *   }
 * }
 * }
 * </pre>
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 */
public interface MIter extends Iter {
  /**
   * Remove the object the iterator currently points to.
   * 
   * Note that, usually, the iterator will now point to a different object, very
   * often to the previous one (but this is not guaranteed!)
   */
  void remove();
}
