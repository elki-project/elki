package experimentalcode.erich.newdblayer.ids.generic;

import java.util.Collection;
import java.util.TreeSet;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDs;
import experimentalcode.erich.newdblayer.ids.TreeSetModifiableDBIDs;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
// TODO: implement this optimized for integers?
public class GenericTreeSetModifiableDBIDs extends TreeSet<DBID> implements TreeSetModifiableDBIDs {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public GenericTreeSetModifiableDBIDs(int initialCapacity) {
    super();
  }

  /**
   * Constructor without extra hints
   */
  public GenericTreeSetModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public GenericTreeSetModifiableDBIDs(DBIDs c) {
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
