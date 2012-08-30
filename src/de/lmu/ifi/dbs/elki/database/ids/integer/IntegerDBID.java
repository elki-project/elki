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

import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.persistent.FixedSizeByteBufferSerializer;

/**
 * Database ID object.
 * 
 * While this currently is just an Integer, it should be avoided to store the
 * object IDs in regular integers to reduce problems if this API ever changes
 * (for example if someone needs to support {@code long}, it should not require
 * changes in too many places!)
 * 
 * In particular, a developer should not make any assumption of these IDs being
 * consistent across multiple results/databases.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf DynamicSerializer
 * @apiviz.composedOf StaticSerializer
 */
final class IntegerDBID implements DBID, IntegerDBIDRef {
  /**
   * The actual object ID.
   */
  final protected int id;

  /**
   * Constructor from integer id.
   * 
   * @param id integer id.
   */
  protected IntegerDBID(int id) {
    super();
    this.id = id;
  }

  /**
   * Constructor from integer id.
   * 
   * @param id integer id.
   */
  protected IntegerDBID(Integer id) {
    super();
    this.id = id;
  }

  @Override
  public DBID deref() {
    return this;
  }

  /**
   * Return the integer value of the object ID.
   * 
   * @return integer id
   */
  @Override
  public int getIntegerID() {
    return this.id;
  }

  @Override
  public String toString() {
    return Integer.toString(id);
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof IntegerDBID)) {
      if(obj instanceof DBIDRef) {
        LoggingUtil.warning("Programming error: DBID.equals(DBIDRef) is not well-defined. use sameDBID!", new Throwable());
      }
      return false;
    }
    IntegerDBID other = (IntegerDBID) obj;
    return this.id == other.id;
  }

  @Override
  public int compareTo(DBIDRef o) {
    final int anotherVal = DBIDFactory.FACTORY.asInteger(o);
    return (this.id < anotherVal ? -1 : (this.id == anotherVal ? 0 : 1));
  }

  @Override
  public DBIDArrayIter iter() {
    return new DBIDItr();
  }

  @Override
  public DBID get(int i) {
    if(i != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return this;
  }

  @Override
  public boolean contains(DBIDRef o) {
    return DBIDFactory.FACTORY.asInteger(o) == id;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return (id == DBIDFactory.FACTORY.asInteger(key)) ? 0 : -1;
  }

  /**
   * Pseudo iterator for DBIDs interface.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements DBIDArrayIter, IntegerDBIDRef {
    /**
     * Iterator position: We use an integer so we can support retract().
     */
    int pos = 0;

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public void advance(int count) {
      pos += count;
    }

    @Override
    public void retract() {
      pos--;
    }

    @Override
    public void seek(int off) {
      pos = off;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public int getIntegerID() {
      return IntegerDBID.this.id;
    }

    @Override
    public DBID deref() {
      return IntegerDBID.this;
    }

    @Override
    public boolean valid() {
      return (pos == 0);
    }

    @Override
    public boolean equals(Object other) {
      if(other instanceof DBID) {
        LoggingUtil.warning("Programming error detected: DBIDItr.equals(DBID). Use sameDBID()!", new Throwable());
      }
      return super.equals(other);
    }

    @Override
    public String toString() {
      return Integer.toString(getIntegerID());
    }
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  /**
   * Dynamic sized serializer, using varint.
   * 
   * @author Erich Schubert
   */
  public static class DynamicSerializer implements ByteBufferSerializer<DBID> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected DynamicSerializer() {
      super();
    }

    @Override
    public DBID fromByteBuffer(ByteBuffer buffer) throws UnsupportedOperationException {
      return new IntegerDBID(ByteArrayUtil.readSignedVarint(buffer));
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DBID object) throws UnsupportedOperationException {
      ByteArrayUtil.writeSignedVarint(buffer, ((IntegerDBID) object).id);
    }

    @Override
    public int getByteSize(DBID object) throws UnsupportedOperationException {
      return ByteArrayUtil.getSignedVarintSize(((IntegerDBID) object).id);
    }
  }

  /**
   * Static sized serializer, using regular integers.
   * 
   * @author Erich Schubert
   */
  public static class StaticSerializer implements FixedSizeByteBufferSerializer<DBID> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected StaticSerializer() {
      super();
    }

    @Override
    public DBID fromByteBuffer(ByteBuffer buffer) throws UnsupportedOperationException {
      return new IntegerDBID(buffer.getInt());
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DBID object) throws UnsupportedOperationException {
      buffer.putInt(((IntegerDBID) object).id);
    }

    @Override
    public int getByteSize(DBID object) throws UnsupportedOperationException {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return ByteArrayUtil.SIZE_INT;
    }
  }

  /**
   * The public instance to use for dynamic serialization.
   */
  public static final DynamicSerializer dynamicSerializer = new DynamicSerializer();

  /**
   * The public instance to use for static serialization.
   */
  public static final StaticSerializer staticSerializer = new StaticSerializer();
}