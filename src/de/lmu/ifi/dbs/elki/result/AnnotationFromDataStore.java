package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;

/**
 * Annotations backed by a DataStore.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data type to store.
 */
// TODO: make serializable.
public class AnnotationFromDataStore<T> extends BasicResult implements Relation<T> {
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
  public T get(DBID objID) {
    return map.get(objID);
  }

  @Override
  public DBIDs getDBIDs() {
    return DBIDUtil.makeUnmodifiable(dbids);
  }

  @Override
  public IterableIterator<DBID> iterDBIDs() {
    return IterableUtil.fromIterator(dbids.iterator());
  }

  @Override
  public int size() {
    return dbids.size();
  }

  @SuppressWarnings("unused")
  @Override
  public void set(DBID id, T val) {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unused")
  @Override
  public void delete(DBID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Database getDatabase() {
    return null;
  }

  @Override
  public SimpleTypeInformation<T> getDataTypeInformation() {
    return assoc.getType();
  }
}