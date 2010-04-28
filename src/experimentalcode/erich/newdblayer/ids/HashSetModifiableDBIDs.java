package experimentalcode.erich.newdblayer.ids;

import java.util.Collection;
import java.util.HashSet;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
// TODO: implement this optimized for integers?
public class HashSetModifiableDBIDs extends HashSet<DBID> implements ModifiableDBIDs {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public HashSetModifiableDBIDs(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor without extra hints
   */
  public HashSetModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public HashSetModifiableDBIDs(DBIDs c) {
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
