package experimentalcode.erich.newdblayer.ids.integer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import experimentalcode.erich.newdblayer.ids.DBID;

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
 */
public class IntegerDBID implements DBID {
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
  @Deprecated
  public DBID getID() {
    return this;
  }

  @Override
  @Deprecated
  public void setID(@SuppressWarnings("unused") DBID id) {
    throw new UnsupportedOperationException("IDs in new DB layer are static.");
  }

  @Override
  public int compareTo(DBID o) {
    return o.getIntegerID() - this.id;
  }

  @Override
  public Collection<DBID> asCollection() {
    ArrayList<DBID> ret = new ArrayList<DBID>(1);
    ret.add(this);
    return ret;
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
      assert(first);
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
}