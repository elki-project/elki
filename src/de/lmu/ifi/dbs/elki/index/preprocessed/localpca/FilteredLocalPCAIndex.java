package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;

/**
 * Interface for an index providing local PCA results.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Vector type
 */
public interface FilteredLocalPCAIndex<NV extends NumberVector<?, ?>> extends LocalProjectionIndex<NV, PCAFilteredResult> {
  /**
   * Get the precomputed local PCA for a particular object ID.
   * 
   * @param objid Object ID
   * @return Matrix
   */
  @Override
  public PCAFilteredResult getLocalProjection(DBID objid);

  /**
   * Factory interface
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses FilteredLocalPCAIndex oneway - - «create»
   */
  public static interface Factory<NV extends NumberVector<?, ?>, I extends FilteredLocalPCAIndex<NV>> extends LocalProjectionIndex.Factory<NV, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param <V> Actual vector type
     * @param database Database type
     * 
     * @return Index
     */
    @Override
    public I instantiate(Database<NV> database);
  }
}