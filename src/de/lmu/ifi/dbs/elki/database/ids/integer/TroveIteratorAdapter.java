package de.lmu.ifi.dbs.elki.database.ids.integer;

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

import gnu.trove.iterator.TIntIterator;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Adapter for using GNU Trove iterators.
 * 
 * @author Erich Schubert
 */
class TroveIteratorAdapter implements Iterator<DBID> {
  /**
   * The actual iterator.
   */
  private TIntIterator iterator;

  /**
   * Constructor.
   * 
   * @param iterator Trove iterator
   */
  protected TroveIteratorAdapter(TIntIterator iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public DBID next() {
    return new IntegerDBID(iterator.next());
  }

  @Override
  public void remove() {
    iterator.remove();
  }
}