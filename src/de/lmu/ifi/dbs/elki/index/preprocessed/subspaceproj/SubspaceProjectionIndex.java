package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;

/**
 * Interface for an index providing local subspaces.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Vector type
 */
public interface SubspaceProjectionIndex<NV extends NumberVector<?, ?>, P extends ProjectionResult> extends LocalProjectionIndex<NV, P> {
  /**
   * Get the precomputed local subspace for a particular object ID.
   * 
   * @param objid Object ID
   * @return Matrix
   */
  @Override
  public P getLocalProjection(DBID objid);

  /**
   * Factory interface
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SubspaceProjectionIndex oneway - - «create»
   */
  public static interface Factory<NV extends NumberVector<?, ?>, I extends SubspaceProjectionIndex<NV, ?>> extends LocalProjectionIndex.Factory<NV, I> {
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