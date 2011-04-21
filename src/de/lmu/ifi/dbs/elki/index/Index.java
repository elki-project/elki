package de.lmu.ifi.dbs.elki.index;

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
 */
public interface Index extends Result {
  /**
   * Get the underlying page file (or a proxy), for access counts.
   * 
   * @return page file
   */
  public PageFileStatistics getPageFileStatistics();

  /**
   * Inserts the specified object into this index.
   * 
   * @param id the object to be inserted
   */
  public void insert(DBID id);

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param ids the objects to be inserted
   */
  public void insertAll(DBIDs ids);

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
   * Get the indexed relation.
   * 
   * @return Relation this index is bound to
   */
  public Relation<?> getRelation();
}