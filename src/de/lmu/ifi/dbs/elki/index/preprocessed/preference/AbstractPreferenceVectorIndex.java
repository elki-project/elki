package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract base class for preference vector based algorithms.
 * 
 * @author Erich Schubert
 *
 * @param <NV> Number vector
 */
public abstract class AbstractPreferenceVectorIndex<NV extends NumberVector<?, ?>> extends AbstractPreprocessorIndex<NV, BitSet> implements PreferenceVectorIndex<NV> {
  /**
   * Database we are attached to
   */
  final protected Database<NV> database;

  /**
   * Constructor.
   * 
   * @param database Database to use
   */
  public AbstractPreferenceVectorIndex(Database<NV> database) {
    super();
    this.database = database;
  }

  /**
   * Preprocessing step.
   */
  abstract protected void preprocess();

  @Override
  public BitSet getPreferenceVector(DBID objid) {
    if(storage == null) {
      preprocess();
    }
    return storage.get(objid);
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractPreferenceVectorIndex oneway - - «create»
   */
  public static abstract class Factory<V extends NumberVector<?, ?>, I extends PreferenceVectorIndex<V>> implements PreferenceVectorIndex.Factory<V, I>, Parameterizable {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super();
      config = config.descend(this);
    }

    @Override
    public abstract I instantiate(Database<V> database);
  }
}