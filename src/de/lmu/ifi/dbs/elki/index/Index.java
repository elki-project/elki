package de.lmu.ifi.dbs.elki.index;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Interface defining the minimum requirements for all index classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * 
 * @param <O> the type of objects to be stored in the index
 */
public interface Index<O> extends Result {
  /**
   * Get the underlying page file (or a proxy), for access counts.
   * 
   * @return page file
   */
  public PageFileStatistics getPageFileStatistics();

  /**
   * Inserts the specified object into this index.
   * 
   * @param object the object to be inserted
   */
  public void insert(DBID id, O object);

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param objects the objects to be inserted
   */
  public void insertAll(ArrayDBIDs ids, List<O> objects);

  /**
   * Deletes the specified object from this index.
   * 
   * @param id Object to remove
   * @return true if this index did contain the object, false otherwise
   */
  public boolean delete(DBID id);

  /**
   * Deletes the specified objects from this index.
   * 
   * @param ids Objects to remove
   */
  public void deleteAll(DBIDs ids);

  /**
   * Get the indexed representation.
   * 
   * @return Representation this index is bound to
   */
  public Relation<O> getRepresentation();
}