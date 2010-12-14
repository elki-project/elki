package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;

/**
 * Interface for an index providing local PCA results.
 * 
 * @author Erich Schubert
 *
 * @param <NV> Vector type
 */
public interface FilteredLocalPCAIndex<NV extends NumberVector<?, ?>> extends Index<NV> {
  /**
   * Get the precomputed local PCA for a particular object ID.
   * 
   * @param objid Object ID
   * @return Matrix
   */
  public PCAFilteredResult get(DBID objid);
}
