package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;

import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for precomputed neighborhoods.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractPrecomputedNeighborhood implements NeighborSetPredicate {
  /**
   * The data
   */
  protected DataStore<DBIDs> store;
  
  /**
   * Constructor.
   *
   * @param store the actual data.
   */
  public AbstractPrecomputedNeighborhood(DataStore<DBIDs> store) {
    super();
    this.store = store;
  }

  @Override
  public DBIDs getNeighborDBIDs(DBID reference) {
    DBIDs neighbors = store.get(reference);
    if(neighbors != null) {
      return neighbors;
    }
    else {
      // Use just the object itself.
      if(getLogger().isDebugging()) {
        getLogger().warning("No neighbors for object " + reference);
      }
      return reference;
    }
  }

  /**
   * The logger to use for error reporting.
   * 
   * @return Logger
   */
  abstract protected Logging getLogger();

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has AbstractPrecomputedNeighborhood
   */
  public abstract static class Factory<O> implements NeighborSetPredicate.Factory<O> {
    // Nothing to add.
  }
}