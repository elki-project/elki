package de.lmu.ifi.dbs.elki.database.ids;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.utilities.iterator.Iter;

/**
 * Iterator for DBIDs.
 * 
 * Important note: this iterator has a <em>significantly</em> different syntax
 * and semantics than the Java iterators. It is much more aligned with C than
 * with Java, but at the same time, the syntax is much more compatible with for
 * loops.
 * 
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
 *
 * @apiviz.landmark
 */
public interface DBIDIter extends DBIDRef, Iter {
  /**
   * Get the referenced {@link DBID}.
   * 
   * Efficiency note: this may require materialization of a DBID object - if
   * possible, use DBIDRef based APIs instead.
   *
   * USe {@link DBIDUtil#deref} instead!
   * 
   * @return referenced DBID
   */
  @Override
  DBIDRef deref();
}
