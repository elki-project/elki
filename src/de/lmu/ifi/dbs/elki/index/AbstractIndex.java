package de.lmu.ifi.dbs.elki.index;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * Abstract base class for indexes with some implementation defaults.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public abstract class AbstractIndex<O> implements Index<O> {
  /**
   * The representation we are bound to.
   */
  protected final Relation<O> rep;
  
  /**
   * Constructor.
   *
   * @param rep Representation
   */
  public AbstractIndex(Relation<O> rep) {
    super();
    this.rep = rep;
  }
  
  @Override
  public Relation<O> getRepresentation() {
    return rep;
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
  public void insert(DBID id, O object) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public void insertAll(ArrayDBIDs id, List<O> objects) {
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