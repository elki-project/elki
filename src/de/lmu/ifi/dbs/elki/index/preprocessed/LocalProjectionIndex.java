package de.lmu.ifi.dbs.elki.index.preprocessed;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;

/**
 * Abstract index interface for local projections
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ProjectionResult
 * 
 * @param <V> Vector type
 * @param <P> Projection result type
 */
public interface LocalProjectionIndex<V extends NumberVector<?, ?>, P extends ProjectionResult> extends Index<V> {
  /**
   * Get the precomputed local projection for a particular object ID.
   * 
   * @param objid Object ID
   * @return local projection
   */
  public P getLocalProjection(DBID objid);

  /**
   * Factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses LocalProjectionIndex oneway - - «create»
   * 
   * @param <V> Vector type
   * @param <I> Index type
   */
  public static interface Factory<V extends NumberVector<?, ?>, I extends LocalProjectionIndex<V, ?>> extends IndexFactory<V, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param database Database type
     * 
     * @return Index
     */
    @Override
    public I instantiate(Database<V> database);
  }
}