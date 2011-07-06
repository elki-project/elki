package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;

/**
 * Represents a single representation. This is attached to a DBIDs object, which
 * you are supposed to manage first. I.e. put the new DBID in, then invoke
 * set(), remove the DBID, then delete().
 * 
 * TODO: is this semantic sane?
 * 
 * @author Erich Schubert
 * 
 * @param <O> Data type
 */
public class MaterializedRelation<O> extends AbstractHierarchicalResult implements Relation<O> {
  /**
   * Our database
   */
  private final Database database;

  /**
   * The class of objects we store.
   */
  private final SimpleTypeInformation<O> type;

  /**
   * Map to hold the objects of the database.
   */
  private final WritableDataStore<O> content;

  /**
   * The DBIDs this is supposed to be defined for.
   * 
   * Note: we only keep an unmodifiable reference.
   */
  private final StaticDBIDs ids;

  /**
   * Constructor.
   * 
   * @param database Database
   * @param type Type information
   * @param ids IDs
   */
  public MaterializedRelation(Database database, SimpleTypeInformation<O> type, DBIDs ids) {
    super();
    this.database = database;
    this.type = type;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.content = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, type.getRestrictionClass());
  }

  /**
   * Constructor.
   * 
   * @param database Database
   * @param type Type information
   * @param ids IDs
   * @param content Content
   */
  public MaterializedRelation(Database database, SimpleTypeInformation<O> type, DBIDs ids, WritableDataStore<O> content) {
    super();
    this.database = database;
    this.type = type;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.content = content;
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public O get(DBID id) {
    return content.get(id);
  }

  @Override
  public void set(DBID id, O val) {
    assert (ids.contains(id));
    content.put(id, val);
  }

  /**
   * Delete an objects values.
   * 
   * @param id ID to delete
   */
  @Override
  public void delete(DBID id) {
    assert (!ids.contains(id));
    content.delete(id);
  }

  @Override
  public StaticDBIDs getDBIDs() {
    return ids;
  }

  @Override
  public IterableIterator<DBID> iterDBIDs() {
    return IterableUtil.fromIterable(ids);
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public SimpleTypeInformation<O> getDataTypeInformation() {
    return type;
  }

  @Override
  public String getLongName() {
    return type.toString();
  }

  @Override
  public String getShortName() {
    return "representation";
  }
}