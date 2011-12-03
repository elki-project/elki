package de.lmu.ifi.dbs.elki.database.ids;

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
/**
 * Iterator for DBIDs.
 * 
 * Important note: this iterator has a <em>significantly</em> different syntax
 * and semantics than the Java iterators. It is much more aligned with C than
 * with Java, but at the same time, the syntax is much more compatible with for
 * loops.
 * 
 * Usage example: <blockquote><code>{@code 
 * for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
 *   iter.getDBID();
 * }
 * }</code></blockquote>
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
 */
public interface DBIDIter {
  /**
   * Returns true if the iterator currently points to a valid object.
   * 
   * @return a <code>boolean</code> value
   */
  public boolean valid();

  /**
   * Moves the iterator forward to the next entry.
   * 
   * @throws java.util.NoSuchElementException if the iterator is already
   *         exhausted
   */
  public void advance();

  /**
   * Return the integer value of the object ID, if possible.
   * 
   * @return integer id
   */
  public int getIntegerID();

  /**
   * Get the current DBID.
   * 
   * @return current DBID
   */
  public DBID getDBID();
}