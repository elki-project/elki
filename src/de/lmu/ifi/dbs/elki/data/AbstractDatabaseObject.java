package de.lmu.ifi.dbs.elki.data;


import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Abstract super class for all database objects. Provides the required access
 * methods for the unique object id.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractDatabaseObject implements DatabaseObject {
  /**
   * The unique id of this object.
   */
  private DBID id;

  @Override
  public final DBID getID() {
    return id;
  }

  @Override
  public void setID(DBID id) {
    this.id = id;
  }
}