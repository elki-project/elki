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
package de.lmu.ifi.dbs.elki.database.ids;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Empty DBID collection.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @composed - - - EmptyDBIDIterator
 */
public class EmptyDBIDs implements ArrayStaticDBIDs, SetDBIDs {
  /**
   * Empty DBID iterator.
   */
  public static final EmptyDBIDIterator EMPTY_ITERATOR = new EmptyDBIDIterator();

  /**
   * Constructor.
   */
  protected EmptyDBIDs() {
    super();
  }

  @Override
  public boolean contains(DBIDRef o) {
    return false;
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
  public DBIDVar assignVar(int index, DBIDVar var) {
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public DBIDArrayMIter iter() {
    return EMPTY_ITERATOR;
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return -1; // Not found
  }

  @Override
  public ArrayDBIDs slice(int begin, int end) {
    return this;
  }
  
  @Override
  public void forEach(Consumer<? super DBIDRef> action) {
    // Empty
  }

  /**
   * Iterator for empty DBIDs-
   *
   * @author Erich Schubert
   */
  protected static class EmptyDBIDIterator implements DBIDArrayMIter {
    @Override
    public boolean valid() {
      return false;
    }

    @Override
    public EmptyDBIDIterator advance() {
      assert (false) : "Misplaced call to advance()";
      return this;
    }

    @Override
    public int internalGetIndex() {
      throw new NoSuchElementException();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof DBID) {
        LoggingUtil.warning("Programming error detected: DBIDItr.equals(DBID). Use sameDBID()!", new Throwable());
      }
      return super.equals(other);
    }

    @Override
    public void remove() {
      throw new NoSuchElementException();
    }

    @Override
    public EmptyDBIDIterator advance(int count) {
      assert (count != 0) : "Misplaced call to advance()";
      return this;
    }

    @Override
    public EmptyDBIDIterator retract() {
      assert (false) : "Misplaced call to retract()";
      return this;
    }

    @Override
    public EmptyDBIDIterator seek(int off) {
      // Ignore
      return this;
    }

    @Override
    public int getOffset() {
      return 0;
    }

    @Override
    public void setDBID(DBIDRef val) {
      throw new UnsupportedOperationException();
    }
  }
}
