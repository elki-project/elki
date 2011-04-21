package de.lmu.ifi.dbs.elki.index.preprocessed.snn;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;

/**
 * Interface for an index providing nearest neighbor sets.
 * 
 * @author Erich Schubert
 */
public interface SharedNearestNeighborIndex<O> extends Index {
  /**
   * Get the precomputed nearest neighbors
   * 
   * @param objid Object ID
   * @return Neighbor DBIDs
   */
  public TreeSetDBIDs getNearestNeighborSet(DBID objid);

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
   * @apiviz.uses SharedNearestNeighborIndex oneway - - «create»
   * 
   * @param <O> The input object type
   * @param <I> Index type produced
   */
  public static interface Factory<O, I extends SharedNearestNeighborIndex<O>> extends IndexFactory<O, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param database Database type
     * 
     * @return Index
     */
    @Override
    public I instantiate(Relation<O> database);

    /**
     * Get the number of neighbors
     * 
     * @return NN size
     */
    public int getNumberOfNeighbors();
  }
}