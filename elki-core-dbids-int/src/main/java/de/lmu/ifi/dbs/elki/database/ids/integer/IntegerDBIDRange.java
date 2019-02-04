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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;

/**
 * Representing a DBID range allocation.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
final class IntegerDBIDRange implements IntegerDBIDs, DBIDRange, SetDBIDs {
  /**
   * Start value.
   */
  protected final int start;

  /**
   * Length value.
   */
  protected final int len;

  /**
   * Constructor.
   *
   * @param start Range start
   * @param len Range length
   */
  protected IntegerDBIDRange(int start, int len) {
    super();
    this.start = start;
    this.len = len;
  }

  @Override
  public int size() {
    return len;
  }

  @Override
  public boolean contains(DBIDRef o) {
    int oid = o.internalGetIndex();
    if (oid < start) {
      return false;
    }
    if (oid >= start + len) {
      return false;
    }
    return true;
  }

  @Override
  public DBID get(int i) {
    if (i > len || i < 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return DBIDFactory.FACTORY.importInteger(start + i);
  }

  /**
   * For storage array offsets.
   *
   * @param dbid ID reference
   * @return array offset
   */
  @Override
  public int getOffset(DBIDRef dbid) {
    return dbid.internalGetIndex() - start;
  }

  @Override
  public DBIDVar assignVar(int index, DBIDVar var) {
    if (var instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) var).internalSetIndex(start + index);
      return var;
    } else {
      // Much less efficient:
      var.set(get(index));
      return var;
    }
  }

  @Override
  public int binarySearch(DBIDRef key) {
    int keyid = DBIDUtil.asInteger(key);
    if (keyid < start) {
      return -1;
    }
    final int off = keyid - start;
    if (off < len) {
      return off;
    }
    return -(len + 1);
  }

  @Override
  public String toString() {
    return "[" + start + " to " + (start + len - 1) + "]";
  }

  @Override
  public int mapDBIDToOffset(DBIDRef dbid) {
    return dbid.internalGetIndex() - start;
  }

  @Override
  public ArrayDBIDs slice(int begin, int end) {
    return new IntegerDBIDRange(begin + start, end - begin);
  }

  @Override
  public Itr iter() {
    return new Itr(start, len);
  }

  /**
   * Iterator in ELKI/C++ style.
   *
   * @author Erich Schubert
   */
  private final static class Itr implements IntegerDBIDArrayIter {
    /**
     * Current position.
     */
    private int pos;

    /**
     * Interval length.
     */
    final private int len;

    /**
     * Interval start.
     */
    final private int start;

    /**
     * Constructor.
     *
     * @param start Interval start
     * @param len Interval length
     */
    public Itr(final int start, final int len) {
      this.start = start;
      this.len = len;
    }

    @Override
    public boolean valid() {
      return pos < len && pos >= 0;
    }

    @Override
    public Itr advance() {
      ++pos;
      return this;
    }

    @Override
    public Itr advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public Itr retract() {
      --pos;
      return this;
    }

    @Override
    public Itr seek(int off) {
      pos = off;
      return this;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public int internalGetIndex() {
      return start + pos;
    }

    @Override
    public boolean equals(Object other) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return Integer.toString(internalGetIndex());
    }
  }
}
