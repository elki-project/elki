package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;

/**
 * Interface for an index providing preference vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Vector type
 */
public interface PreferenceVectorIndex<NV extends NumberVector<?, ?>> extends Index<NV> {
  /**
   * Get the precomputed preference vector for a particular object ID.
   * 
   * @param objid Object ID
   * @return Matrix
   */
  public BitSet getPreferenceVector(DBID objid);

  /**
   * Factory interface
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PreferenceVectorIndex oneway - - «create»
   */
  public static interface Factory<V extends NumberVector<?, ?>, I extends PreferenceVectorIndex<V>> extends IndexFactory<V, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param <V> Actual vector type
     * @param database Database type
     * 
     * @return Index
     */
    @Override
    public I instantiate(Database<V> database);
  }
}