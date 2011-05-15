package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.UpdatableDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;

/**
 * Pseudo-representation that is the object ID itself.
 * 
 * @author Erich Schubert
 */
public class DBIDView extends AbstractHierarchicalResult implements Relation<DBID> {
  /**
   * The database
   */
  private final Database database;

  /**
   * The ids object
   */
  private final DBIDs ids;

  /**
   * Constructor.
   * 
   * @param database
   * @param ids
   */
  public DBIDView(Database database, DBIDs ids) {
    super();
    this.database = database;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public String getLongName() {
    return "Database IDs";
  }

  @Override
  public String getShortName() {
    return "DBID";
  }

  @Override
  public DBID get(DBID id) {
    assert (ids.contains(id));
    return id;
  }

  @SuppressWarnings("unused")
  @Override
  public void set(DBID id, DBID val) {
    throw new UnsupportedOperationException("DBIDs cannot be changed.");
  }

  @Override
  public void delete(DBID id) {
    if(database instanceof UpdatableDatabase) {
      ((UpdatableDatabase) database).delete(id);
    }
    else {
      throw new UnsupportedOperationException("Deletions are not supported.");
    }
  }

  @Override
  public SimpleTypeInformation<DBID> getDataTypeInformation() {
    return TypeUtil.DBID;
  }

  @Override
  public DBIDs getDBIDs() {
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
}