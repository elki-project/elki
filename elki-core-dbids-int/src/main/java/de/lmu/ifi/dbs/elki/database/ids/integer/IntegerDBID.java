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

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.io.FixedSizeByteBufferSerializer;

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
 * @since 0.4.0
 *
 * @composed - - - DynamicSerializer
 * @composed - - - StaticSerializer
 */
final class IntegerDBID implements DBID, IntegerDBIDRef {
  /**
   * The actual object ID.
   */
  protected final int id;

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
    this.id = id.intValue();
  }

  /**
   * Return the integer value of the object ID.
   *
   * @return integer id
   */
  @Override
  public int internalGetIndex() {
    return this.id;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String toString() {
    return (id != Integer.MIN_VALUE) ? Integer.toString(id) : "null";
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  @Deprecated
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof IntegerDBID)) {
      if(obj instanceof DBIDRef) {
        LoggingUtil.warning("Programming error: DBID.equals(DBIDRef) is not well-defined. Use DBIDUtil.equal() instead!", new Throwable());
      }
      return false;
    }
    IntegerDBID other = (IntegerDBID) obj;
    return this.id == other.id;
  }

  @Override
  public int compareTo(DBIDRef o) {
    final int anotherVal = o.internalGetIndex();
    return (this.id < anotherVal ? -1 : (this.id == anotherVal ? 0 : 1));
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  @Override
  public DBID get(int i) {
    if(i != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return this;
  }

  @Override
  public DBIDVar assignVar(int index, DBIDVar var) {
    if(index != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    var.set(this);
    return var;
  }

  @Override
  public boolean contains(DBIDRef o) {
    return o.internalGetIndex() == id;
  }

  @Override
  public int binarySearch(DBIDRef key) {
    final int other = key.internalGetIndex();
    return (other == id) ? 0 : (other < id) ? -1 : -2;
  }

  @Override
  public ArrayDBIDs slice(int begin, int end) {
    if(begin == 0 && end == 1) {
      return this;
    }
    else {
      return DBIDUtil.EMPTYDBIDS;
    }
  }

  /**
   * Pseudo iterator for DBIDs interface.
   *
   * @author Erich Schubert
   */
  protected class Itr implements DBIDArrayIter, IntegerDBIDRef {
    /**
     * Iterator position: We use an integer so we can support retract().
     */
    int pos = 0;

    @Override
    public Itr advance() {
      pos++;
      return this;
    }

    @Override
    public Itr advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public Itr retract() {
      pos--;
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
      return IntegerDBID.this.id;
    }

    @Override
    public boolean valid() {
      return (pos == 0);
    }

    @Override
    public int hashCode() {
      // Override, because we also are overriding equals.
      return super.hashCode();
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
      return Integer.toString(internalGetIndex());
    }
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
    public DynamicSerializer() {
      super();
    }

    @Override
    public DBID fromByteBuffer(ByteBuffer buffer) throws IOException {
      return new IntegerDBID(ByteArrayUtil.readSignedVarint(buffer));
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DBID object) throws IOException {
      ByteArrayUtil.writeSignedVarint(buffer, ((IntegerDBID) object).id);
    }

    @Override
    public int getByteSize(DBID object) throws IOException {
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
    public StaticSerializer() {
      super();
    }

    @Override
    public DBID fromByteBuffer(ByteBuffer buffer) throws IOException {
      return new IntegerDBID(buffer.getInt());
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DBID object) throws IOException {
      buffer.putInt(((IntegerDBID) object).id);
    }

    @Override
    public int getByteSize(DBID object) throws IOException {
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
  public static final ByteBufferSerializer<DBID> DYNAMIC_SERIALIZER = new DynamicSerializer();

  /**
   * The public instance to use for static serialization.
   */
  public static final FixedSizeByteBufferSerializer<DBID> STATIC_SERIALIZER = new StaticSerializer();
}
