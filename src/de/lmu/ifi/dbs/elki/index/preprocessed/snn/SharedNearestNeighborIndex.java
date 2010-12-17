package de.lmu.ifi.dbs.elki.index.preprocessed.snn;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;

/**
 * Interface for an index providing nearest neighbor sets.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface SharedNearestNeighborIndex<O extends DatabaseObject> extends Index<O> {
  /**
   * Get the precomputed nearest neighbors
   * 
   * @param objid Object ID
   * @return Neighbor DBIDs
   */
  public SetDBIDs getNearestNeighborSet(DBID objid);

  /**
   * Get the number of neighbors
   * 
   * @return NN size
   */
  public int getNumberOfNeighbors();

  /**
   * Factory interface
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses FilteredLocalPCAIndex oneway - - «create»
   */
  public static interface Factory<O extends DatabaseObject, I extends SharedNearestNeighborIndex<O>> extends IndexFactory<O, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param <V> Actual vector type
     * @param database Database type
     * 
     * @return Index
     */
    @Override
    public I instantiate(Database<O> database);

    /**
     * Get the number of neighbors
     * 
     * @return NN size
     */
    public int getNumberOfNeighbors();
  }
}