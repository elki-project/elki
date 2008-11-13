package de.lmu.ifi.dbs.elki.data;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Interface for a collection of database references (IDs).
 * The two main implementations are {@link DatabaseObjectGroupCollection}
 * and {@link DatabaseObjectGroupArray} backed by an array or Collection
 * respectively. Performance varies depending on the actual choice.
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public interface DatabaseObjectGroup extends Iterable<Integer> {
  /**
   * Get the database the objects live in.
   * 
   * @return Database
   */
  //TODO: O should be made a generic of the class itself!
  public <O extends DatabaseObject> Database<O> getDatabase();
  
  /**
   * Retrieve collection access to the IDs
   * 
   * @return a collection of IDs
   */
  public Collection<Integer> getIDs();
  
  /**
   * Retrieve Iterator access to the IDs.
   * 
   * @return an iterator for the IDs
   */
  public Iterator<Integer> iterator();
  
  /**
   * Retrieve the collection / data size.
   */
  public int size();
}
