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
 * Iterator interface for more than one return value.
 * 
 * The Java standard {@link java.util.Iterator} interface has some drawbacks:
 * <ul>
 * <li>the only way to get the current value is to advance the iterator</li>
 * <li>the iterator can only point to a single value</li>
 * <li>the iterator can only return objects, not primitives</li>
 * </ul>
 * 
 * This iterator interface is a bit more flexible. For example on a distance
 * list, we can have a single type of iterator that allows access to the
 * distance, the object ID or the combination of both.
 * 
 * In some situations, this can save the creation of many small objects, which
 * put load on the garbage collector. This super interface does not have a "get"
 * operation, which is to come from specialized interfaces instead.
 * 
 * Usage example:
 * 
 * <pre>
 * {@code 
 * for (Iter iter = ids.iter(); iter.valid(); iter.advance()) {
 *   iter.doSomething();
 * }
 * }
 * </pre>
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @opt nodefillcolor LemonChiffon
 */
public interface Iter {
  /**
   * Returns true if the iterator currently points to a valid object.
   * 
   * @return a <code>boolean</code> value, whether the position is valid.
   */
  boolean valid();

  /**
   * Moves the iterator forward to the next entry.
   * 
   * @return The iterator itself.
   */
  Iter advance();
}
