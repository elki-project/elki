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

import java.util.Iterator;

/**
 * Utility function to wrap an Iterator as iterable.
 * 
 * @author Erich Schubert
 */
public final class IterableUtil {
  /**
   * Wrap an iterator as Iterable.
   * 
   * @param <C> Type restriction
   * @param iter Iterator
   * @return Iterable wrapper
   */
  public static <C> IterableIterator<C> fromIterator(Iterator<C> iter) {
    return new IterableIteratorAdapter<C>(iter);
  }

  /**
   * Wrap an Iterable as IterableIterator
   * 
   * @param <C> Type restriction
   * @param iter Iterator
   * @return Iterable wrapper
   */
  public static <C> IterableIterator<C> fromIterable(Iterable<C> iter) {
    return new IterableIteratorAdapter<C>(iter);
  }
}
