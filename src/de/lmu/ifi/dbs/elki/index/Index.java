package de.lmu.ifi.dbs.elki.index;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Interface defining the minimum requirements for all index classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * 
 * @param <O> the type of DatabaseObject to be stored in the index
 */
public interface Index<O extends DatabaseObject> extends Result {
  /**
   * Get the underlying page file (or a proxy), for access counts.
   * 
   * @return page file
   */
  public PageFileStatistics getPageFileStatistics();
  
  /**
   * Inserts the specified object into this index.
   * 
   * @param object the vector to be inserted
   */
  public void insert(O object);

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param objects the objects to be inserted
   */
  public void insert(List<O> objects);

  /**
   * Deletes the specified object from this index.
   * 
   * @param object the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  public boolean delete(O object);
}