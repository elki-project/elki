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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.lmu.ifi.dbs.elki.utilities.iterator.EmptyIterator;

/**
 * Empty DBID collection.
 * 
 * @author Erich Schubert
 */
class EmptyDBIDs extends AbstractList<DBID> implements ArrayStaticDBIDs {
  /**
   * Empty DBID iterator
   */
  public static final EmptyDBIDIterator EMPTY_ITERATOR = new EmptyDBIDIterator();

  /**
   * Constructor.
   */
  protected EmptyDBIDs() {
    super();
  }

  @Override
  public Collection<DBID> asCollection() {
    return new ArrayList<DBID>(0);
  }

  @Override
  public boolean contains(Object o) {
    return false;
  }

  @Override
  public Iterator<DBID> iterator() {
    return EmptyIterator.STATIC();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public DBID get(int i) {
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public DBIDIter iter() {
    return EMPTY_ITERATOR;
  }

  /**
   * Iterator for empty DBIDs
   * 
   * @author Erich Schubert
   */
  protected static class EmptyDBIDIterator implements DBIDIter {
    @Override
    public boolean valid() {
      return false;
    }

    @Override
    public void advance() {
      assert (false) : "Misplaced call to advance()";
    }

    @Override
    public int getIntegerID() {
      throw new NoSuchElementException();
    }

    @Override
    public DBID getDBID() {
      throw new NoSuchElementException();
    }
  }
}