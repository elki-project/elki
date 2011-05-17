package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * Abstract base class for indexes with some implementation defaults.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type stored in the index
 */
public abstract class AbstractIndex<O> implements Index {
  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;
  
  /**
   * Constructor.
   *
   * @param relation Relation indexed
   */
  public AbstractIndex(Relation<O> relation) {
    super();
    this.relation = relation;
  }

  @Override
  abstract public String getLongName();

  @Override
  abstract public String getShortName();
  
  @Override
  public PageFileStatistics getPageFileStatistics() {
    // TODO: move this into a separate interface?
    // By default, we are not file based - no statistics available
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public void insert(DBID id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public void insertAll(DBIDs ids) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public boolean delete(DBID id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public void deleteAll(DBIDs id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }
}