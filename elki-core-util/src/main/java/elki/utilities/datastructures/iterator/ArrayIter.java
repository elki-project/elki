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
package elki.utilities.datastructures.iterator;

/**
 * Array iterators can also go backwards and seek.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 */
public interface ArrayIter extends Iter {
  @Override
  ArrayIter advance();

  /**
   * Moves the iterator forward or backward by the given offset.
   * 
   * @param count offset to move forward or backwards
   * @return Iterator
   */
  ArrayIter advance(int count);

  /**
   * Moves the iterator backward to the previous entry.
   * 
   * @return Iterator
   */
  ArrayIter retract();

  /**
   * Moves the iterator to the given position
   * 
   * @param off Seek offset
   * @return Iterator
   */
  ArrayIter seek(int off);

  /**
   * Get current iterator offset.
   * 
   * @return Iterator position
   */
  int getOffset();
}
