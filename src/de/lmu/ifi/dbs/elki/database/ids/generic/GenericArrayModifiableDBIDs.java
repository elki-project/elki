package de.lmu.ifi.dbs.elki.database.ids.generic;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;


/**
 * Array-oriented implementation of a modifiable DBID collection.
 * 
 * This should only be instantiated by a {@link de.lmu.ifi.dbs.elki.database.ids.DBIDFactory}!
 * 
 * Use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newArray}!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.ids.DBID
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
  public boolean addDBIDs(DBIDs ids) {
    return super.addAll(ids.asCollection());
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    return super.removeAll(ids.asCollection());
  }
}