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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Iterator that joins multiple existing iterators into one.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype adapter
 * @apiviz.uses Iterator oneway
 *
 * @param <E> Entry type
 */
public class MergedIterator<E> implements IterableIterator<E> {
  /**
   * All the iterators we process
   */
  final Iterator<Iterator<E>> iterators;
  
  /**
   * The iterator we are currently processing
   */
  Iterator<E> current = null;

  /**
   * The last iterator we returned an object for, for remove()
   */
  Iterator<E> last = null;
  
  /**
   * Main constructor.
   * 
   * @param iterators Iterators
   */
  public MergedIterator(Iterator<Iterator<E>> iterators) {
    super();
    this.iterators = iterators;
  }

  /**
   * Auxillary constructor with Collections
   * 
   * @param iterators Iterators
   */
  public MergedIterator(Collection<Iterator<E>> iterators) {
    this(iterators.iterator());
  }

  /**
   * Auxillary constructor with arrays
   * 
   * @param iterators Iterators
   */
  public MergedIterator(Iterator<E>... iterators) {
    this(Arrays.asList(iterators).iterator());
  }

  @Override
  public boolean hasNext() {
    while((current != null && current.hasNext()) || iterators.hasNext()) {
      // Next element in current iterator?
      if (current != null && current.hasNext()) {
        return true;
      }
      // advance master iterator and retry
      current = iterators.next();
    }
    return false;
  }

  @Override
  public E next() {
    while (!current.hasNext()) {
      current = iterators.next();
    }
    last = current;
    return current.next();
  }

  @Override
  public void remove() {
    if (last == null) {
      throw new RuntimeException("Iterator.remove() called without next()");
    }
    last.remove();
  }

  @Override
  public Iterator<E> iterator() {
    return this;
  }
}
