package experimentalcode.erich.newdblayer.ids.generic;

import java.util.ArrayList;
import java.util.Collection;

import experimentalcode.erich.newdblayer.ids.ArrayModifiableDBIDs;
import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDs;

/**
 * Array-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
// TODO: implement this optimized for integers?
public class GenericArrayModifiableDBIDs extends ArrayList<DBID> implements ArrayModifiableDBIDs  {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public GenericArrayModifiableDBIDs(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor without extra hints
   */
  public GenericArrayModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public GenericArrayModifiableDBIDs(DBIDs c) {
    super(c.asCollection());
  }

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public boolean addAll(DBIDs ids) {
    return super.addAll(ids.asCollection());
  }

  @Override
  public boolean containsAll(DBIDs ids) {
    return super.containsAll(ids.asCollection());
  }

  @Override
  public boolean removeAll(DBIDs ids) {
    return super.removeAll(ids.asCollection());
  }
}