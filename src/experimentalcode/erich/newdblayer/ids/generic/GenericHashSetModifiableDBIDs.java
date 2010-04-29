package experimentalcode.erich.newdblayer.ids.generic;

import java.util.Collection;
import java.util.HashSet;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDs;
import experimentalcode.erich.newdblayer.ids.HashSetModifiableDBIDs;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
// TODO: implement this optimized for integers?
public class GenericHashSetModifiableDBIDs extends HashSet<DBID> implements HashSetModifiableDBIDs {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public GenericHashSetModifiableDBIDs(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor without extra hints
   */
  public GenericHashSetModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public GenericHashSetModifiableDBIDs(DBIDs c) {
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
