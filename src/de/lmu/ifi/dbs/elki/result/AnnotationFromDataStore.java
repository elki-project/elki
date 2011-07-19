package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Annotations backed by a DataStore.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type to store.
 */
// TODO: make serializable.
public class AnnotationFromDataStore<T> extends BasicResult implements AnnotationResult<T> {
  /**
   * Store the hashmap for results.
   */
  private DataStore<? extends T> map;
  
  /**
   * Store Association ID
   */
  private AssociationID<T> assoc;

  /**
   * The DBIDs we are defined for.
   */
  private DBIDs dbids;

  /**
   * Constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param assoc Association
   * @param map Map
   * @param dbids The DBIDs we are defined for
   */
  public AnnotationFromDataStore(String name, String shortname, AssociationID<T> assoc, DataStore<? extends T> map, DBIDs dbids) {
    super(name, shortname);
    this.map = map;
    this.assoc = assoc;
    this.dbids = dbids;
  }

  @Override
  public AssociationID<T> getAssociationID() {
    return assoc;
  }

  @Override
  public T getValueFor(DBID objID) {
    return map.get(objID);
  }

  @Override
  public DBIDs getDBIDs() {
    return DBIDUtil.makeUnmodifiable(dbids);
  }
}