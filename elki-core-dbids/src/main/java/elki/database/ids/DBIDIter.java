/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.ids;

import elki.utilities.datastructures.iterator.Iter;

/**
 * Iterator for DBIDs.
 * <p>
 * Important note: this iterator has a <em>significantly</em> different syntax
 * and semantics than the Java iterators. It is much more aligned with C than
 * with Java, but at the same time, the syntax is much more compatible with for
 * loops.
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code 
 * for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
 *   Object o = relation.get(iter); // Many interfaces allow direct use
 *   DBID id = DBIDUtil.deref(iter); // Materialize only if you need to!
 * }
 * }
 * </pre>
 *
 * We list some fundamental differences.
 * <ul>
 * <li>{@link DBIDIter#valid() iter.valid()} refers to the current element,
 * {@code Iterator.next()} to the next.</li>
 * <li>{@link DBIDIter#advance() iter.advance()} does not return an element. Use
 * {@code get...} to access it.</li>
 * <li>{@code DBIDIter.get...} do not advance the iterator.</li>
 * </ul>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 */
public interface DBIDIter extends DBIDRef, Iter {
  @Override
  DBIDIter advance();

  /**
   * Add all remaining elements of an iterator to an existing collection.
   * 
   * @param <T> Collection type
   * @param collection Existing collection
   * @return The existing collection
   */
  default <T extends ModifiableDBIDs> T addTo(T collection) {
    while(valid()) {
      collection.add(this);
      advance();
    }
    return collection;
  }
}
