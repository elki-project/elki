package de.lmu.ifi.dbs.elki.data;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Collection-backed group of database object (references) 
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <C> Collection type used.
 * @param <O> DatabaseObject type used.
 */
public final class DatabaseObjectGroupCollection<C extends Collection<Integer>> implements DatabaseObjectGroup {
  /**
   * The database referenced.
   */
  public Database<?> database;
  
  /**
   * Storage for ids.
   */
  public C ids;

  /**
   * Constructor to wrap an existing collection.
   * The collection is not copied, but referenced!
   * 
   * @param ids collection to use.
   */
  public DatabaseObjectGroupCollection(Database<?> db, C ids) {
    super();
    this.database = db;
    this.ids = ids;
  }

  /**
   * Retrieve the IDs as collection.
   */
  @Override
  public C getIDs() {
    return ids;
  }

  /**
   * Retrieve an iterator over the IDs.
   */
  @Override
  public Iterator<Integer> iterator() {
    return ids.iterator();
  }

  /**
   * Return the backing collections size.
   */
  @Override
  public int size() {
    return ids.size();
  }
  
  /**
   * Retrieve the database used.
   */
  // TODO: a bit hackish to not keep the databaseobject type as a generics of the object
  @SuppressWarnings("unchecked")
  public <O extends DatabaseObject> Database<O> getDatabase() {
    return (Database<O>) database;
  }
}
