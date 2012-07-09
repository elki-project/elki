package de.lmu.ifi.dbs.elki.utilities.iterator;

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

/**
 * Array iterators can also go backwards and seek.
 * 
 * @author Erich Schubert
 */
public interface ArrayIter extends Iter {
  /**
   * Get current iterator offset.
   * 
   * @return Iterator position
   */
  public int getOffset();
  
  /**
   * Moves the iterator forward or backward by the given offset.
   * 
   * @param count offset to move forward or backwards
   */
  public void advance(int count);

  /**
   * Moves the iterator backward to the previous entry.
   */
  public void retract();

  /**
   * Moves the iterator to the given position
   * 
   * @param off Seek offset
   */
  public void seek(int off);
}