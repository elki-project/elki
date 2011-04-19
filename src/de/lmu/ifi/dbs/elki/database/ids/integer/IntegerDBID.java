package de.lmu.ifi.dbs.elki.database.ids.integer;

import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

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
class IntegerDBID extends AbstractList<DBID> implements DBID {
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
      return false;
    }
    IntegerDBID other = (IntegerDBID) obj;
    return this.id == other.id;
  }

  @Override
  public int compareTo(DBID o) {
    int thisVal = this.id;
    int anotherVal = o.getIntegerID();
    return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
  }

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public boolean contains(Object o) {
    return this.equals(o);
  }

  @Override
  public Iterator<DBID> iterator() {
    return new Itr();
  }

  @Override
  public int size() {
    return 1;
  }

  /**
   * Pseudo iterator for DBIDs interface.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Itr implements Iterator<DBID> {
    /**
     * Whether we've already returned our object.
     */
    boolean first = true;

    @Override
    public boolean hasNext() {
      return first == true;
    }

    @Override
    public DBID next() {
      assert (first);
      first = false;
      return IntegerDBID.this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public DBID get(int i) {
    if(i == 0) {
      return this;
    }
    else {
      throw new ArrayIndexOutOfBoundsException();
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
  public static class StaticSerializer implements ByteBufferSerializer<DBID> {
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
    public int getByteSize(@SuppressWarnings("unused") DBID object) throws UnsupportedOperationException {
      return ByteArrayUtil.SIZE_INT;
    }
  }

  /**
   * The public instance to use for dynamic serialization.
   */
  public final static DynamicSerializer dynamicSerializer = new DynamicSerializer();

  /**
   * The public instance to use for static serialization.
   */
  public final static StaticSerializer staticSerializer = new StaticSerializer();
}