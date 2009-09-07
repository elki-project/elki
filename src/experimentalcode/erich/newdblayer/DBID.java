package experimentalcode.erich.newdblayer;

/**
 * Database ID object.
 * 
 * While this currently is just an Integer, it should be avoided to store the
 * object IDs in regular integers to reduce problems if this API ever changes
 * (for example if someone needs to support Long, it should not require changes
 * in too many places!)
 * 
 * In particular, a developer should not make any assumption of these IDs being
 * consistent.
 * 
 * @author Erich Schubert
 */
public final class DBID {
  /**
   * The actual object ID.
   */
  protected int id;

  /**
   * Constructor from integer id.
   * 
   * @param id integer id.
   */
  public DBID(int id) {
    super();
    this.id = id;
  }

  /**
   * Constructor from integer id.
   * 
   * @param id integer id.
   */
  public DBID(Integer id) {
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
    if(!(obj instanceof DBID)) {
      return false;
    }
    DBID other = (DBID) obj;
    return this.id == other.id;
  }
}