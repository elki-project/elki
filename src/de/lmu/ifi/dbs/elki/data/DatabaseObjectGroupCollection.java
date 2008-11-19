package de.lmu.ifi.dbs.elki.data;

import java.util.Collection;
import java.util.Iterator;

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
   * Storage for ids.
   */
  public C ids;

  /**
   * Constructor to wrap an existing collection.
   * The collection is not copied, but referenced!
   * 
   * @param ids collection to use.
   */
  public DatabaseObjectGroupCollection(C ids) {
    super();
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
}
