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
 * Iterator that filters results by type.
 * 
 * @author Erich Schubert
 *
 * @param <IN> Input datatype
 * @param <OUT> Output datatype
 */
public class TypeFilterIterator<IN, OUT extends IN> extends AbstractFilteredIterator<IN, OUT> implements Iterator<OUT> {
  /**
   * Class restriction
   */
  private Class<? super OUT> filterClass;

  /**
   * Parent iterator
   */
  private Iterator<IN> parent;

  /**
   * Constructor.
   * 
   * @param filterClass Filter
   * @param parent Parent collection
   */
  public TypeFilterIterator(Class<? super OUT> filterClass, Iterable<IN> parent) {
    super();
    this.filterClass = filterClass;
    this.parent = parent.iterator();
  }

  /**
   * Constructor.
   * 
   * @param filterClass Filter
   * @param parent Parent iterator
   */
  public TypeFilterIterator(Class<? super OUT> filterClass, Iterator<IN> parent) {
    super();
    this.filterClass = filterClass;
    this.parent = parent;
  }

  @Override
  protected Iterator<IN> getParentIterator() {
    return parent;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected OUT testFilter(IN nextobj) {
    try {
      return (OUT) filterClass.cast(nextobj);
    }
    catch(ClassCastException e) {
      return null;
    }
  }
}