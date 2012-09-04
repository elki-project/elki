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

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract class to build filtered views on Iterables.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype decorator
 * @apiviz.composedOf Iterator
 * 
 * @param <IN> Input type
 * @param <OUT> Output type
 */
public abstract class AbstractFilteredIterator<IN, OUT extends IN> implements Iterator<OUT> {
  /**
   * The iterator to use.
   */
  Iterator<IN> itr = null;

  /**
   * The next object to return.
   */
  OUT nextobj = null;

  /**
   * Constructor.
   */
  public AbstractFilteredIterator() {
    super();
  }

  /**
   * Init the iterators.
   */
  protected void init() {
    this.itr = getParentIterator();
    if (this.itr == null) {
      throw new AbortException("Filtered iterator has 'null' parent.");
    }
  }

  /**
   * Get an iterator for the actual data. Used in initialization.
   * 
   * @return iterator
   */
  protected abstract Iterator<IN> getParentIterator();

  /**
   * Test the filter predicate for a new object.
   * 
   * @param nextobj Object to test
   * @return cast object when true, {@code null} otherwise
   */
  protected abstract OUT testFilter(IN nextobj);

  /**
   * Find the next visualizer.
   */
  private void updateNext() {
    if(itr == null) {
      init();
    }
    nextobj = null;
    while(itr.hasNext()) {
      IN v = itr.next();
      nextobj = testFilter(v);
      if(nextobj != null) {
        break;
      }
    }
  }

  @Override
  public boolean hasNext() {
    if(itr == null) {
      updateNext();
    }
    return (nextobj != null);
  }

  @Override
  public OUT next() {
    OUT ret = this.nextobj;
    updateNext();
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}