package de.lmu.ifi.dbs.elki.database.ids.generic;

import java.util.Collection;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * This should only be instantiated by a {@link de.lmu.ifi.dbs.elki.database.ids.DBIDFactory}!
 * 
 * Use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newTreeSet}!
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
  public boolean addDBIDs(DBIDs ids) {
    return super.addAll(ids.asCollection());
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    return super.removeAll(ids.asCollection());
  }
}
